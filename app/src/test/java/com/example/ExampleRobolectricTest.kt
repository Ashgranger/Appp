package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppAnalyticsTracker
import com.example.data.BrowserPreferencesState
import com.example.data.EncryptedPasswordStore
import com.example.data.FeedbackStore
import com.example.data.ThemeMode
import com.example.engine.OmniboxMathEvaluator
import com.example.engine.RenderingOptimizerScripts
import com.example.engine.SearchSuggestionService
import com.example.engine.SwiftShieldEngine
import com.example.ui.screens.isApkDownload
import com.example.viewmodel.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Fevro Browser`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Fevro Browser", appName)
    }

    @Test
    fun `swift shield blocks subdomains and respects site allowlist`() {
        val shield = SwiftShieldEngine()
        assertTrue(shield.shouldBlock("https://ads.doubleclick.net/banner.js", "news.example.com"))
        assertFalse(shield.shouldBlock("https://news.example.com/article.html", "news.example.com"))

        shield.updateAllowlist(listOf("news.example.com"))
        assertFalse(shield.shouldBlock("https://ads.doubleclick.net/banner.js", "news.example.com"))
    }

    @Test
    fun `filter list parser handles hosts and EasyList syntax`() {
        val sample = """
            # Comment line
            ! AdBlock comment
            0.0.0.0 tracker.badads.io
            127.0.0.1 www.pixel.metrics.net
            ||promo.popups.org^${'$'}third-party
            localhost
        """.trimIndent()

        val parsed = SwiftShieldEngine.parseFilterListText(sample)
        assertEquals(listOf("tracker.badads.io", "pixel.metrics.net", "promo.popups.org"), parsed)
    }

    @Test
    fun `omnibox math evaluator solves expressions and ignores urls`() {
        assertEquals("400", OmniboxMathEvaluator.evaluate("(24 * 15) + 40"))
        assertEquals("16", OmniboxMathEvaluator.evaluate("2^4"))
        assertNull(OmniboxMathEvaluator.evaluate("10 / 0"))
        assertNull(OmniboxMathEvaluator.evaluate("https://example.com"))
        assertNull(OmniboxMathEvaluator.evaluate("42"))
    }

    @Test
    fun `swift shield strips tracking parameters while preserving functional query params`() {
        val shield = SwiftShieldEngine()
        val dirty = "https://example.com/article?id=42&utm_source=twitter&utm_medium=social&fbclid=abc123&gclid=xyz&page=2"
        val res = shield.sanitizeTrackingParameters(dirty)
        assertEquals(4, res.strippedCount)
        assertTrue(res.cleanUrl.contains("id=42"))
        assertTrue(res.cleanUrl.contains("page=2"))
        assertFalse(res.cleanUrl.contains("utm_source"))
        assertFalse(res.cleanUrl.contains("fbclid"))
        assertFalse(res.cleanUrl.contains("gclid"))
    }

    @Test
    fun `swift shield blocks popup redirect domains and ad script heuristics`() {
        val shield = SwiftShieldEngine()
        assertTrue(shield.isPopupOrAdRedirect("https://go.popads.net/popunder?id=1", "news.example.com"))
        assertFalse(shield.isPopupOrAdRedirect("https://news.example.com/sports", "news.example.com"))
        assertTrue(shield.shouldBlock("https://cdn.unknown-site.net/scripts/prebid.js", "news.example.com"))
        assertTrue(shield.shouldBlock("https://telemetry.unknown-site.net/v1/event", "news.example.com"))
    }

    @Test
    fun `encrypted password store saves updates matches subdomains and respects never-save`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = EncryptedPasswordStore(context)
        store.clearAllCredentials()

        val saved = store.upsertCredential("https://www.github.com/login", "https://github.com/login", "dev@example.com", "Secret!123")
        assertEquals("github.com", saved.host)
        assertEquals(1, store.credentialsFlow.value.size)
        assertTrue(store.hasExactCredential("github.com", "dev@example.com", "Secret!123"))

        // Subdomain match for autofill
        val matches = store.findForHost("https://gist.github.com/auth")
        assertEquals(1, matches.size)
        assertEquals("dev@example.com", matches.first().username)

        // Update existing credential for same host+username
        store.upsertCredential("github.com", "https://github.com/login", "dev@example.com", "NewPass!456")
        assertEquals(1, store.credentialsFlow.value.size)
        assertEquals("NewPass!456", store.findForHost("github.com").first().password)

        // Never-save list
        store.addNeverSaveHost("https://bank.example.org/signin")
        assertTrue(store.isHostNeverSave("bank.example.org"))
        store.removeNeverSaveHost("bank.example.org")
        assertFalse(store.isHostNeverSave("bank.example.org"))

        // Delete
        store.deleteCredential(saved.id)
        assertTrue(store.credentialsFlow.value.isEmpty())
    }

    @Test
    fun `strong password generator respects length and character sets`() {
        val generated = EncryptedPasswordStore.generateStrongPassword(
            length = 20,
            includeUppercase = true,
            includeDigits = true,
            includeSymbols = true
        )
        assertEquals(20, generated.length)
        assertTrue(generated.any { it.isLowerCase() })
        assertTrue(generated.any { it.isUpperCase() })
        assertTrue(generated.any { it.isDigit() })
        assertTrue(generated.any { !it.isLetterOrDigit() })
    }

    @Test
    fun `autofill script safely quotes special characters and apk detector identifies apps`() {
        val script = RenderingOptimizerScripts.buildPasswordAutofillScript("user\"'test", "p@ss\\word\"1")
        assertTrue(script.contains("setNativeValue"))
        assertTrue(script.contains("HTMLInputElement"))

        assertTrue(isApkDownload("SwiftApp_v2.apk", "application/octet-stream"))
        assertTrue(isApkDownload("bundle.xapk", ""))
        assertTrue(isApkDownload("installer", "application/vnd.android.package-archive"))
        assertFalse(isApkDownload("report.pdf", "application/pdf"))
    }

    @Test
    fun `feedback store saves entries locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = FeedbackStore(context)
        val entry = store.submit(5, "Passwords", "Fast local autofill!")
        assertEquals(5, entry.rating)
        assertEquals("Passwords", entry.category)
        assertTrue(store.entriesFlow.value.any { it.id == entry.id })
    }

    @Test
    fun `analytics tracker records sessions streaks downloads and excludes incognito`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tracker = AppAnalyticsTracker(context)
        tracker.resetLocalStats()

        val dayMillis = 24L * 60L * 60L * 1000L
        val today = System.currentTimeMillis()
        val yesterday = today - dayMillis

        tracker.recordAppOpen(nowMillis = yesterday)
        tracker.recordAppOpen(nowMillis = today)
        tracker.recordAppOpen(nowMillis = today)

        tracker.recordPageVisit(isIncognito = false)
        tracker.recordPageVisit(isIncognito = true) // Must be excluded

        tracker.recordDownloadStarted("app_release.apk", "application/vnd.android.package-archive", isApk = true)
        tracker.recordDownloadStarted("guide.pdf", "application/pdf", isApk = false)

        tracker.recordPasswordSaved()
        tracker.recordPasswordAutofilled()

        val metrics = tracker.metricsFlow.value
        assertEquals(3, metrics.totalSessions)
        assertEquals(2, metrics.activeDaysCount)
        assertEquals(2, metrics.currentStreakDays)
        assertEquals(7, metrics.last7Days.size)
        assertEquals(2, metrics.last7Days.last().sessions)
        assertEquals(1, metrics.totalPagesVisited)
        assertEquals(2, metrics.totalDownloadsStarted)
        assertEquals(1, metrics.apkDownloadsStarted)
        assertEquals(1, metrics.passwordsSavedCount)
        assertEquals(1, metrics.autofillUsesCount)

        // Reset clears counters while preserving install timestamp
        val installTime = metrics.firstInstalledAt
        assertTrue(installTime > 0L)
        tracker.resetLocalStats(nowMillis = today)
        val afterReset = tracker.metricsFlow.value
        assertEquals(installTime, afterReset.firstInstalledAt)
        assertEquals(0, afterReset.totalSessions)
        assertEquals(0, afterReset.totalDownloadsStarted)
        assertEquals(0, afterReset.totalPagesVisited)
    }

    @Test
    fun `default browser preferences use AMOLED black theme and live search suggestions`() {
        val defaults = BrowserPreferencesState()
        assertEquals(ThemeMode.AMOLED, defaults.themeMode)
        assertTrue(defaults.searchSuggestionsEnabled)
    }

    @Test
    fun `search suggestion service matches engine endpoints and skips urls`() {
        assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "google").contains("suggestqueries.google.com"))
        assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "duckduckgo").contains("duckduckgo.com/ac"))
        assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "bing").contains("api.bing.com/osjson.aspx"))
        assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "brave").contains("search.brave.com/api/suggest"))
        assertTrue(SearchSuggestionService.buildSuggestionUrl("kotlin compose", "wikipedia").contains("action=opensearch"))

        assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("a"))
        assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("https://example.com"))
        assertFalse(SearchSuggestionService.shouldFetchRemoteSuggestions("github.com"))
        assertTrue(SearchSuggestionService.shouldFetchRemoteSuggestions("android browser"))
    }

    @Test
    fun `search suggestion parser handles OpenSearch arrays and DuckDuckGo phrase objects`() {
        val openSearchJson = """["android", ["android", "android studio", "android 16", "android studio"]]"""
        val parsedOpenSearch = SearchSuggestionService.parseSuggestionsJson(openSearchJson, "android")
        assertEquals(listOf("android studio", "android 16"), parsedOpenSearch)

        val ddgJson = """[{"phrase":"privacy"},{"phrase":"privacy browser"},{"phrase":"privacy tips"}]"""
        val parsedDdg = SearchSuggestionService.parseSuggestionsJson(ddgJson, "privacy")
        assertEquals(listOf("privacy browser", "privacy tips"), parsedDdg)

        assertTrue(SearchSuggestionService.parseSuggestionsJson("invalid", "test").isEmpty())
    }

    @Test
    fun `reader json parser extracts editorial blocks and stripped clutter metadata`() {
        val sampleJson = """
            {
              "title": "Clean Reading Mode",
              "byline": "Jane Doe",
              "publishedDate": "2026-11-24",
              "siteName": "example.com",
              "leadImageUrl": "https://example.com/hero.jpg",
              "strippedClutterCount": 14,
              "wordCount": 420,
              "blocks": [
                {"kind": "heading", "text": "Introduction"},
                {"kind": "paragraph", "text": "Readable article prose without sidebars."},
                {"kind": "quote", "text": "Simplicity is prerequisite for reliability."},
                {"kind": "list_item", "text": "Strips ads and navbars"},
                {"kind": "code", "text": "val clean = true"}
              ]
            }
        """.trimIndent()

        val article = BrowserViewModel.parseReaderJson(sampleJson, "https://example.com/post", "Fallback Title")
        assertNotNull(article)
        assertEquals("Clean Reading Mode", article!!.title)
        assertEquals("Jane Doe", article.byline)
        assertEquals("2026-11-24", article.publishedDate)
        assertEquals("https://example.com/hero.jpg", article.leadImageUrl)
        assertEquals(14, article.strippedClutterCount)
        assertEquals(5, article.blocks.size)
        assertEquals(3, article.readingMinutes)
    }
}


