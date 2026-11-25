const { describe, it, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

let testEnv;

function pastTimestamp(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

describe("SwiftWeb Firestore Security Rules", () => {
  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: emulatorHost,
        port: emulatorPort,
        rules: fs.readFileSync("./firestore.rules", "utf8"),
      },
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  const aliceDb = () => testEnv.authenticatedContext("alice_uid").firestore();
  const bobDb = () => testEnv.authenticatedContext("bob_uid").firestore();
  const unauthDb = () => testEnv.unauthenticatedContext().firestore();

  // --- Unauthenticated Access ---

  it("denies unauthenticated access to user profile and all subcollections", async () => {
    const db = unauthDb();
    await assertFails(db.collection("users").doc("alice_uid").get());
    await assertFails(db.collection("users").get());
    await assertFails(
      db.collection("users").doc("alice_uid").collection("bookmarks").get()
    );
    await assertFails(
      db.collection("users").doc("alice_uid").collection("history").get()
    );
    await assertFails(
      db.collection("users").doc("alice_uid").collection("searchEngines").get()
    );
    await assertFails(
      db.collection("users").doc("alice_uid").collection("allowlist").get()
    );
    await assertFails(
      db
        .collection("users")
        .doc("alice_uid")
        .collection("customFilterRules")
        .get()
    );
  });

  // --- User Profile ---

  it("allows owner CRUD on UserProfile and denies listing users or cross-user access", async () => {
    const db = aliceDb();
    const bob = bobDb();
    const ref = db.collection("users").doc("alice_uid");
    const created = pastTimestamp(10000);

    await assertSucceeds(
      ref.set({
        userId: "alice_uid",
        displayName: "Alice",
        email: "alice@example.com",
        createdAt: created,
        updatedAt: created,
      })
    );

    await assertSucceeds(ref.get());
    await assertFails(db.collection("users").get()); // no user enumeration
    await assertFails(bob.collection("users").doc("alice_uid").get());

    // Partial update preserving immutable userId and createdAt
    await assertSucceeds(
      ref.update({
        displayName: "Alice Swift",
        photoUrl: "https://example.com/avatar.png",
        updatedAt: pastTimestamp(1000),
      })
    );

    // Reject tampering with immutable userId or createdAt
    await assertFails(
      ref.update({
        userId: "bob_uid",
        updatedAt: pastTimestamp(500),
      })
    );
    await assertFails(
      ref.update({
        createdAt: pastTimestamp(500),
        updatedAt: pastTimestamp(500),
      })
    );

    // Reject cross-user update and delete
    await assertFails(
      bob.collection("users").doc("alice_uid").update({
        displayName: "Hacked",
        updatedAt: pastTimestamp(500),
      })
    );
    await assertFails(bob.collection("users").doc("alice_uid").delete());
    await assertSucceeds(ref.delete());
  });

  // --- Bookmarks & Speed Dials ---

  it("allows owner CRUD on bookmarks and enforces schema, immutability, and no extra id key", async () => {
    const db = aliceDb();
    const bob = bobDb();
    const col = db.collection("users").doc("alice_uid").collection("bookmarks");
    const docRef = col.doc("bm_wikipedia_01");
    const created = pastTimestamp(10000);

    // Valid Kotlin-aligned bookmark create (NO @DocumentId `id` field)
    await assertSucceeds(
      docRef.set({
        userId: "alice_uid",
        title: "Wikipedia",
        url: "https://en.wikipedia.org",
        folder: "Speed Dial",
        speedDial: true,
        createdAt: created,
        updatedAt: created,
      })
    );

    await assertSucceeds(docRef.get());
    await assertSucceeds(col.orderBy("createdAt", "desc").get());

    // Valid partial update (toggle speedDial)
    await assertSucceeds(
      docRef.update({
        speedDial: false,
        updatedAt: pastTimestamp(1000),
      })
    );

    // Reject extra "id" key (@DocumentId safety)
    await assertFails(
      col.doc("bm_bad_id_field").set({
        id: "bm_bad_id_field",
        userId: "alice_uid",
        title: "Bad",
        url: "https://example.com",
        folder: "General",
        speedDial: false,
        createdAt: created,
        updatedAt: created,
      })
    );

    // Reject future timestamp
    await assertFails(
      col.doc("bm_future_ts").set({
        userId: "alice_uid",
        title: "Future",
        url: "https://example.com",
        folder: "General",
        speedDial: false,
        createdAt: new Date(Date.now() + 60_000),
        updatedAt: pastTimestamp(1000),
      })
    );

    // Reject empty title
    await assertFails(
      docRef.update({
        title: "",
        updatedAt: pastTimestamp(500),
      })
    );

    // Reject cross-user read, list, create, update, delete
    const bobCol = bob
      .collection("users")
      .doc("alice_uid")
      .collection("bookmarks");
    await assertFails(bobCol.doc("bm_wikipedia_01").get());
    await assertFails(bobCol.get());
    await assertFails(
      bobCol.doc("bm_bob_inject").set({
        userId: "alice_uid",
        title: "Injected",
        url: "https://evil.example",
        folder: "General",
        speedDial: false,
        createdAt: created,
        updatedAt: created,
      })
    );
    await assertFails(
      bobCol.doc("bm_wikipedia_01").update({
        title: "Tampered",
        updatedAt: pastTimestamp(500),
      })
    );
    await assertFails(bobCol.doc("bm_wikipedia_01").delete());

    await assertSucceeds(docRef.delete());
  });

  // --- Browsing History ---

  it("allows owner CRUD on history and blocks cross-user access and url tampering", async () => {
    const db = aliceDb();
    const bob = bobDb();
    const col = db.collection("users").doc("alice_uid").collection("history");
    const docRef = col.doc("hist_entry_01");

    await assertSucceeds(
      docRef.set({
        userId: "alice_uid",
        title: "Android Developers",
        url: "https://developer.android.com",
        visitedAt: pastTimestamp(5000),
      })
    );

    await assertSucceeds(col.orderBy("visitedAt", "desc").limit(20).get());
    await assertSucceeds(
      docRef.update({
        title: "Android Developers Docs",
        visitedAt: pastTimestamp(1000),
      })
    );

    // Reject url modification on history update
    await assertFails(
      docRef.update({
        url: "https://other.example.com",
        visitedAt: pastTimestamp(500),
      })
    );

    // Reject cross-user access
    await assertFails(
      bob.collection("users").doc("alice_uid").collection("history").get()
    );
    await assertFails(
      bob
        .collection("users")
        .doc("alice_uid")
        .collection("history")
        .doc("hist_entry_01")
        .delete()
    );

    await assertSucceeds(docRef.delete());
  });

  // --- Custom Search Engines ---

  it("allows owner CRUD on custom search engines and rejects cross-user access", async () => {
    const db = aliceDb();
    const bob = bobDb();
    const col = db
      .collection("users")
      .doc("alice_uid")
      .collection("searchEngines");
    const docRef = col.doc("custom_kagi_01");

    await assertSucceeds(
      docRef.set({
        userId: "alice_uid",
        name: "Kagi",
        searchUrlTemplate: "https://kagi.com/search?q={query}",
        createdAt: pastTimestamp(5000),
      })
    );

    await assertSucceeds(col.get());
    await assertSucceeds(
      docRef.update({
        name: "Kagi Search",
      })
    );

    await assertFails(
      bob.collection("users").doc("alice_uid").collection("searchEngines").get()
    );
    await assertSucceeds(docRef.delete());
  });

  // --- Allowlist & Custom Filter Rules ---

  it("allows owner create/list/delete on allowlist and customFilterRules and forbids updates", async () => {
    const db = aliceDb();
    const bob = bobDb();

    const allowRef = db
      .collection("users")
      .doc("alice_uid")
      .collection("allowlist")
      .doc("host_example_com");
    await assertSucceeds(
      allowRef.set({
        userId: "alice_uid",
        host: "example.com",
        createdAt: pastTimestamp(5000),
      })
    );
    await assertSucceeds(
      db.collection("users").doc("alice_uid").collection("allowlist").get()
    );
    await assertFails(allowRef.update({ host: "other.com" }));
    await assertFails(
      bob.collection("users").doc("alice_uid").collection("allowlist").get()
    );
    await assertSucceeds(allowRef.delete());

    const ruleRef = db
      .collection("users")
      .doc("alice_uid")
      .collection("customFilterRules")
      .doc("rule_tracker_net");
    await assertSucceeds(
      ruleRef.set({
        userId: "alice_uid",
        domain: "tracker.net",
        createdAt: pastTimestamp(5000),
      })
    );
    await assertSucceeds(
      db
        .collection("users")
        .doc("alice_uid")
        .collection("customFilterRules")
        .get()
    );
    await assertFails(ruleRef.update({ domain: "other.net" }));
    await assertFails(
      bob
        .collection("users")
        .doc("alice_uid")
        .collection("customFilterRules")
        .get()
    );
    await assertSucceeds(ruleRef.delete());
  });
});
