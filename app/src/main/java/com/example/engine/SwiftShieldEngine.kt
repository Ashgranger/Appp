package com.example.engine

import android.net.Uri
import android.webkit.WebResourceResponse
import com.example.data.BrowserPreferencesState
import java.io.ByteArrayInputStream

data class SanitizedUrlResult(
    val cleanUrl: String,
    val strippedCount: Int
)

/**
 * SwiftShield Pro: High-performance multi-layer ad, tracker, popup/ad-redirect,
 * cryptominer, and URL tracking parameter blocking engine.
 * Combines curated instant-ready offline baseline rules with live-updated Room filter lists
 * and user-defined custom rules.
 */
class SwiftShieldEngine {

    @Volatile
    private var globalEnabled: Boolean = true

    @Volatile
    private var activeBlockSet: Set<String> = emptySet()

    @Volatile
    private var allowlistedHosts: Set<String> = emptySet()

    @Volatile
    private var dynamicDomains: Set<String> = emptySet()

    @Volatile
    private var blockAds: Boolean = true

    @Volatile
    private var blockTrackers: Boolean = true

    @Volatile
    private var blockSocial: Boolean = false

    @Volatile
    private var blockAnnoyances: Boolean = true

    @Volatile
    private var stripTrackingParams: Boolean = true

    @Volatile
    private var blockPopupsAndRedirects: Boolean = true

    @Volatile
    private var turboMode: Boolean = true

    init {
        rebuildActiveSet()
    }

    fun updatePreferences(prefs: BrowserPreferencesState) {
        globalEnabled = prefs.shieldEnabledGlobally
        blockAds = prefs.blockAds
        blockTrackers = prefs.blockTrackers
        blockSocial = prefs.blockSocial
        blockAnnoyances = prefs.blockAnnoyances
        stripTrackingParams = prefs.stripTrackingParamsEnabled
        blockPopupsAndRedirects = prefs.blockPopupsAndRedirects
        turboMode = prefs.turboModeEnabled
        rebuildActiveSet()
    }

    fun updateDynamicDomains(domains: Collection<String>) {
        dynamicDomains = domains.mapTo(HashSet(domains.size)) { it.lowercase().trim() }
        rebuildActiveSet()
    }

    fun updateAllowlist(hosts: Collection<String>) {
        allowlistedHosts = hosts.mapTo(HashSet(hosts.size)) { normalizeHost(it) }
    }

    fun activeRuleCount(): Int = activeBlockSet.size

    private fun rebuildActiveSet() {
        val merged = HashSet<String>(dynamicDomains.size + 512)
        if (blockAds) {
            merged.addAll(BASELINE_AD_DOMAINS)
            merged.addAll(BASELINE_POPUP_REDIRECT_DOMAINS)
        }
        if (blockTrackers) {
            merged.addAll(BASELINE_TRACKER_DOMAINS)
            merged.addAll(BASELINE_CRYPTOMINER_DOMAINS)
        }
        if (blockSocial) merged.addAll(BASELINE_SOCIAL_DOMAINS)
        if (blockAnnoyances) merged.addAll(BASELINE_ANNOYANCE_DOMAINS)
        merged.addAll(dynamicDomains)
        activeBlockSet = merged
    }

    fun isSiteProtected(pageHost: String?): Boolean {
        if (!globalEnabled) return false
        if (pageHost.isNullOrBlank()) return true
        val norm = normalizeHost(pageHost)
        return !allowlistedHosts.contains(norm)
    }

    /**
     * Strips known cross-site tracking query parameters (utm_*, fbclid, gclid, etc.)
     * while preserving functional parameters like search queries, video IDs, and pagination.
     */
    fun sanitizeTrackingParameters(rawUrl: String): SanitizedUrlResult {
        if (!globalEnabled || !stripTrackingParams) return SanitizedUrlResult(rawUrl, 0)
        if (!rawUrl.contains("?") || (!rawUrl.startsWith("http://", true) && !rawUrl.startsWith("https://", true))) {
            return SanitizedUrlResult(rawUrl, 0)
        }
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return SanitizedUrlResult(rawUrl, 0)
        val host = uri.host?.lowercase()?.removePrefix("www.")
        if (host != null && allowlistedHosts.contains(host)) {
            return SanitizedUrlResult(rawUrl, 0)
        }
        val names = runCatching { uri.queryParameterNames }.getOrNull() ?: return SanitizedUrlResult(rawUrl, 0)
        if (names.isEmpty()) return SanitizedUrlResult(rawUrl, 0)

        var stripped = 0
        val builder = uri.buildUpon().clearQuery()
        for (name in names) {
            val lower = name.lowercase()
            if (isTrackingParameter(lower)) {
                stripped++
            } else {
                val values = uri.getQueryParameters(name)
                if (values.isEmpty()) {
                    builder.appendQueryParameter(name, "")
                } else {
                    for (value in values) {
                        builder.appendQueryParameter(name, value)
                    }
                }
            }
        }
        return if (stripped > 0) {
            SanitizedUrlResult(builder.build().toString(), stripped)
        } else {
            SanitizedUrlResult(rawUrl, 0)
        }
    }

    /**
     * Detects known popunder, ad-redirect, and click-hijack navigation targets.
     */
    fun isPopupOrAdRedirect(targetUrl: String, currentPageHost: String?): Boolean {
        if (!globalEnabled || !blockPopupsAndRedirects) return false
        if (!isSiteProtected(currentPageHost)) return false

        val uri = runCatching { Uri.parse(targetUrl) }.getOrNull() ?: return false
        val targetHost = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val normalizedCurrent = currentPageHost?.let { normalizeHost(it) }.orEmpty()
        if (targetHost == normalizedCurrent) return false

        if (matchesDomainSet(targetHost, BASELINE_POPUP_REDIRECT_DOMAINS)) return true
        if (blockAds && matchesDomainSet(targetHost, BASELINE_AD_DOMAINS)) return true

        val lowerUrl = targetUrl.lowercase()
        if (lowerUrl.contains("/adclick") ||
            lowerUrl.contains("/aclk?") ||
            lowerUrl.contains("/clicktrack") ||
            lowerUrl.contains("/popunder") ||
            lowerUrl.contains("doubleclick.net/pcs/click")
        ) {
            return true
        }
        return false
    }

    /**
     * Evaluates whether a sub-resource request should be blocked.
     * Runs safely on WebView IO threads with zero locking and O(domain labels) complexity.
     */
    fun shouldBlock(requestUrl: String, pageHost: String?): Boolean {
        if (!globalEnabled) return false
        val normalizedPageHost = pageHost?.let { normalizeHost(it) }.orEmpty()
        if (normalizedPageHost.isNotEmpty() && allowlistedHosts.contains(normalizedPageHost)) {
            return false
        }

        val uri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return false
        val reqHost = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val path = uri.path?.lowercase().orEmpty()
        val query = uri.query?.lowercase().orEmpty()

        // First-party resource: allow normal assets, block pure first-party ad/tracker endpoints
        if (reqHost == normalizedPageHost) {
            if (blockAds && (turboMode || path.contains("/pagead/") || path.contains("/adsense/"))) {
                if (FIRST_PARTY_AD_PATH_PATTERNS.any { path.contains(it) }) {
                    return true
                }
            }
            if (blockTrackers && FIRST_PARTY_TRACKER_PATH_PATTERNS.any { path.contains(it) }) {
                return true
            }
            return false
        }

        // O(domain labels) exact and parent-suffix domain check
        if (matchesDomainSet(reqHost, activeBlockSet)) return true

        // Subdomain prefix heuristics for aggressive third-party ad/tracker subdomains
        val firstLabel = reqHost.substringBefore('.')
        if (blockAds && firstLabel in AD_SUBDOMAIN_LABELS) {
            return true
        }
        if (blockTrackers && firstLabel in TRACKER_SUBDOMAIN_LABELS) {
            return true
        }

        // URL path & script heuristics for third-party ad/tracker resources
        if (blockAds && THIRD_PARTY_AD_PATH_PATTERNS.any { path.contains(it) }) {
            return true
        }
        if (blockTrackers) {
            if (THIRD_PARTY_TRACKER_PATH_PATTERNS.any { path.contains(it) }) {
                return true
            }
            if ((query.contains("utm_source=") && path.contains("/collect")) ||
                (query.contains("fbclid=") && path.contains("pixel")) ||
                query.contains("ad_type=") ||
                query.contains("adunit=")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Returns a clean, resource-appropriate empty stub response so blocked scripts,
     * stylesheets, and 1x1 tracking pixels never cause JS exceptions or broken-image icons.
     */
    fun emptyBlockedResponse(requestUrl: String = ""): WebResourceResponse {
        val lowerPath = runCatching { Uri.parse(requestUrl).path?.lowercase().orEmpty() }.getOrDefault("")
        return when {
            lowerPath.endsWith(".js") || lowerPath.endsWith(".mjs") -> WebResourceResponse(
                "application/javascript",
                "utf-8",
                200,
                "OK",
                mapOf("X-SwiftShield" to "Blocked"),
                ByteArrayInputStream("/* SwiftShield */".toByteArray())
            )
            lowerPath.endsWith(".css") -> WebResourceResponse(
                "text/css",
                "utf-8",
                200,
                "OK",
                mapOf("X-SwiftShield" to "Blocked"),
                ByteArrayInputStream(ByteArray(0))
            )
            lowerPath.endsWith(".gif") || lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".webp") -> WebResourceResponse(
                "image/gif",
                null,
                200,
                "OK",
                mapOf("X-SwiftShield" to "Blocked"),
                ByteArrayInputStream(TRANSPARENT_1X1_GIF)
            )
            else -> WebResourceResponse(
                "text/plain",
                "utf-8",
                204,
                "No Content",
                mapOf("X-SwiftShield" to "Blocked"),
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }

    private fun matchesDomainSet(host: String, set: Set<String>): Boolean {
        if (set.contains(host)) return true
        var dotIndex = host.indexOf('.')
        while (dotIndex != -1 && dotIndex + 1 < host.length) {
            val suffix = host.substring(dotIndex + 1)
            if (set.contains(suffix)) return true
            dotIndex = host.indexOf('.', dotIndex + 1)
        }
        return false
    }

    private fun isTrackingParameter(lowerName: String): Boolean {
        if (lowerName in TRACKING_QUERY_PARAMS) return true
        return TRACKING_PARAM_PREFIXES.any { lowerName.startsWith(it) }
    }

    companion object {
        // Transparent 1x1 GIF for cleanly neutering tracking pixels & ad banners
        private val TRANSPARENT_1X1_GIF = byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
            0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x21, 0xF9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
        )

        private val TRACKING_PARAM_PREFIXES = listOf(
            "utm_", "mtm_", "pk_", "hsa_", "oly_", "vero_", "wicked", "mc_"
        )

        private val TRACKING_QUERY_PARAMS = setOf(
            "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
            "twclid", "ttclid", "li_fat_id", "igshid", "igsh", "si",
            "ref_src", "ref_url", "spm", "_hsenc", "_hsmi", "mkt_tok",
            "rb_clickid", "s_cid", "scid", "irclickid", "srsltid",
            "_ga", "_gl", "_ke", "icid", "ncid", "ocid", "zanpid",
            "trk", "trkcampaign", "tracking_id", "clickid", "ad_id", "campaign_id"
        )

        private val AD_SUBDOMAIN_LABELS = setOf(
            "ads", "adserver", "adservice", "adsystem", "adtech", "adclick", "pagead", "popads"
        )

        private val TRACKER_SUBDOMAIN_LABELS = setOf(
            "metrics", "telemetry", "beacon", "pixel", "track", "tracker", "tracking"
        )

        private val FIRST_PARTY_AD_PATH_PATTERNS = listOf(
            "/pagead/", "/adsense/", "/doubleclick/", "/adserver/", "/adsbygoogle",
            "/pubads_impl", "/gpt.js", "/prebid"
        )

        private val FIRST_PARTY_TRACKER_PATH_PATTERNS = listOf(
            "/analytics/collect", "/telemetry/v", "/pixel.gif", "/beacon.js", "/track/event"
        )

        private val THIRD_PARTY_AD_PATH_PATTERNS = listOf(
            "/pagead/", "/adsense/", "/doubleclick/", "/adserver/", "/adsbygoogle",
            "/pubads_impl", "/gpt.js", "/prebid", "/ads.js", "/ad_banner", "/sponsor_ad"
        )

        private val THIRD_PARTY_TRACKER_PATH_PATTERNS = listOf(
            "/gtag/js", "/gtm.js", "/fbevents.js", "/analytics.js", "/pixel.gif",
            "/tr/?id=", "/collect?v=", "/beacon.min.js"
        )

        fun normalizeHost(hostOrUrl: String): String {
            val trimmed = hostOrUrl.trim().lowercase()
            val parsedHost = if (trimmed.contains("://")) {
                runCatching { Uri.parse(trimmed).host }.getOrNull()
            } else {
                trimmed.substringBefore("/")
            }
            return (parsedHost ?: trimmed).removePrefix("www.").trim()
        }

        /**
         * Parses standard hosts files (`0.0.0.0 domain.com`, `127.0.0.1 domain.com`),
         * AdBlock/EasyList domain rules (`||domain.com^`), and plain domain lists.
         */
        fun parseFilterListText(rawText: String, maxRules: Int = 25000): List<String> {
            val results = LinkedHashSet<String>(minOf(maxRules, 4096))
            rawText.lineSequence().forEach { rawLine ->
                if (results.size >= maxRules) return@forEach
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!") || line.startsWith("[")) {
                    return@forEach
                }
                val candidate = when {
                    line.startsWith("||") -> {
                        line.removePrefix("||")
                            .substringBefore("^")
                            .substringBefore("$")
                            .substringBefore("/")
                            .lowercase()
                            .removePrefix("www.")
                    }
                    line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ") -> {
                        line.substringAfter(' ')
                            .trim()
                            .substringBefore(' ')
                            .substringBefore('#')
                            .lowercase()
                            .removePrefix("www.")
                    }
                    !line.contains(' ') && !line.contains('/') && line.contains('.') -> {
                        line.lowercase().removePrefix("www.")
                    }
                    else -> ""
                }
                if (isValidDomain(candidate)) {
                    results.add(candidate)
                }
            }
            return results.toList()
        }

        private fun isValidDomain(domain: String): Boolean {
            if (domain.length < 4 || domain.length > 180) return false
            if (domain == "localhost" || domain == "local" || domain.startsWith(".") || domain.endsWith(".")) return false
            if (!domain.contains('.')) return false
            return domain.all { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '-' }
        }

        val BASELINE_AD_DOMAINS = setOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
            "pagead2.googlesyndication.com", "tpc.googlesyndication.com", "adnxs.com", "adsrvr.org",
            "advertising.com", "adform.net", "adroll.com", "amazon-adsystem.com", "criteo.com",
            "criteo.net", "outbrain.com", "taboola.com", "pubmatic.com", "rubiconproject.com",
            "openx.net", "casalemedia.com", "indexww.com", "smartadserver.com", "sharethrough.com",
            "triplelift.com", "yieldmo.com", "medianet.com", "media.net", "mgid.com", "revcontent.com",
            "bidswitch.net", "contextweb.com", "lijit.com", "sovrn.com", "teads.tv", "zedo.com",
            "adcolony.com", "applovin.com", "chartboost.com", "inmobi.com", "mopub.com", "unityads.unity3d.com",
            "vungle.com", "ironsrc.com", "tapjoy.com", "flurry.com", "ads.yahoo.com", "adtech.de",
            "ads.twitter.com", "ads.linkedin.com", "ads.tiktok.com", "ads.pinterest.com", "ad.doubleclick.net",
            "adclick.g.doubleclick.net", "static.ads-twitter.com", "bat.bing.com", "ads.reddit.com",
            "ad-delivery.net", "adblade.com", "adbutler.com", "adhigh.net", "adition.com", "adkernel.com",
            "admanmedia.com", "admixer.net", "adnium.com", "adocean.pl", "adrecover.com", "adsafeprotected.com",
            "adscale.de", "adsco.re", "adskeeper.co.uk", "adsymptotic.com", "adtarget.io", "adthrive.com",
            "mediavine.com", "ezoic.net", "carbonads.net", "buysellads.com", "serving-sys.com", "2mdn.net",
            "moatads.com", "doubleverify.com", "integralads.com", "spotxchange.com", "springserve.com",
            "3lift.com", "33across.com", "adlightning.com", "adpushup.com", "ads-twitter.com",
            "adsterra.com", "aniview.com", "btloader.com", "connatix.com", "emxdgt.com", "gumgum.com",
            "id5-sync.com", "kargo.com", "liveintent.com", "magnite.com", "nativo.com", "primis.tech",
            "richaudience.com", "seedtag.com", "smilewanted.com", "sonobi.com", "stickyadstv.com",
            "tremorhub.com", "undertone.com", "unruly.co", "yieldlove.com", "zemanta.com"
        )

        val BASELINE_POPUP_REDIRECT_DOMAINS = setOf(
            "popads.net", "popcash.net", "propellerads.com", "exoclick.com", "juicyads.com",
            "onclickads.net", "onclasrv.com", "adcash.com", "hilltopads.net", "clickadu.com",
            "admaven.com", "popunder.net", "trafficjunky.net", "trafficstars.com", "plugrush.com",
            "adsterra.network", "highcpmgate.com", "profitablegatecpm.com", "redirectvoluum.com",
            "trackvoluum.com", "pushame.com", "pushwoosh.com", "izooto.com", "subscribers.com",
            "notifpush.com", "alwingulla.com", "detikads.com", "revjet.com"
        )

        val BASELINE_TRACKER_DOMAINS = setOf(
            "google-analytics.com", "googletagmanager.com", "analytics.google.com", "stats.g.doubleclick.net",
            "hotjar.com", "hotjar.io", "mixpanel.com", "segment.io", "segment.com", "amplitude.com",
            "fullstory.com", "heap.io", "heapanalytics.com", "crazyegg.com", "mouseflow.com", "luckyorange.com",
            "clarity.ms", "mc.yandex.ru", "scorecardresearch.com", "quantserve.com", "chartbeat.com",
            "chartbeat.net", "newrelic.com", "nr-data.net", "bam.nr-data.net", "omtrdc.net", "demdex.net",
            "2o7.net", "bluekai.com", "krxd.net", "rlcdn.com", "exelator.com", "agkn.com", "tapad.com",
            "branch.io", "app.link", "adjust.com", "appsflyer.com", "kochava.com", "singular.net",
            "bugsnag.com", "sentry.io", "browser-intake-datadoghq.com", "logrocket.com", "kissmetrics.com",
            "hubspot.com", "hs-analytics.net", "hs-scripts.com", "marketo.net", "mktoresp.com", "pardot.com",
            "optimizely.com", "vwo.com", "visualwebsiteoptimizer.com", "ab-tasty.com", "braze.com",
            "onesignal.com", "clevertap.com", "leanplum.com", "mparticle.com", "tealiumiq.com", "tiqcdn.com",
            "ensighten.com", "parsely.com", "pixel.wp.com", "stats.wp.com", "events.redditmedia.com",
            "analytics.tiktok.com", "tr.snapchat.com", "sc-static.net", "px.ads.linkedin.com",
            "crwdcntrl.net", "lotame.com", "liveramp.com", "pippio.com", "mathtag.com", "turn.com",
            "eyeota.net", "narrative.io", "permutive.com", "piano.io", "tinypass.com", "snowplowanalytics.com",
            "posthog.com", "pendo.io", "intercom-clicks.com", "driftt.com", "qualtrics.com",
            "medallia.com", "contentsquare.net", "quantummetric.com", "glassboxdigital.io", "dynatrace.com"
        )

        val BASELINE_CRYPTOMINER_DOMAINS = setOf(
            "coinhive.com", "coin-hive.com", "jsecoin.com", "cryptoloot.pro", "crypto-loot.com",
            "minero.cc", "webminepool.com", "authedmine.com", "monerominer.rocks", "ppoi.org"
        )

        val BASELINE_SOCIAL_DOMAINS = setOf(
            "connect.facebook.net", "pixel.facebook.com", "platform.twitter.com", "syndication.twitter.com",
            "platform.linkedin.com", "assets.pinterest.com", "widgets.pinterest.com", "apis.google.com",
            "platform.instagram.com", "addthis.com", "sharethis.com", "addtoany.com", "disqus.com",
            "disquscdn.com", "po.st", "sumo.com", "sumome.com"
        )

        val BASELINE_ANNOYANCE_DOMAINS = setOf(
            "cookielaw.org", "onetrust.com", "cdn.cookielaw.org", "cookiebot.com", "consent.cookiebot.com",
            "trustarc.com", "consent.trustarc.com", "quantcast.mgr.consensu.org", "cmp.quantcast.com",
            "didomi.io", "sdk.privacy-center.org", "usercentrics.eu", "app.usercentrics.eu",
            "sourcepoint.mgr.consensu.org", "sp-prod.net", "privacymanager.io", "osano.com",
            "iubenda.com", "cdn.iubenda.com", "termly.io", "cookieyes.com", "optinmonster.com",
            "privy.com", "sleeknote.com", "wisepops.com", "hellobar.com", "intercom.io", "widget.intercom.io",
            "drift.com", "js.driftt.com", "tawk.to", "embed.tawk.to", "zendesk.com",
            "fundingchoicesmessages.google.com", "consentmanager.net", "ketch.com", "transcend.io"
        )
    }
}
