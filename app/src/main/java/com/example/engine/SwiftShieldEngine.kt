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
 * SwiftShield Pro: High-performance multi-layer ad, video-ad (YouTube/VAST/VPAID/IMA),
 * tracker, popup/ad-redirect, cryptominer, and URL tracking parameter blocking engine.
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
        val merged = HashSet<String>(dynamicDomains.size + 768)
        if (blockAds) {
            merged.addAll(BASELINE_AD_DOMAINS)
            merged.addAll(BASELINE_VIDEO_AD_DOMAINS)
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
            lowerUrl.contains("/pagead/aclk") ||
            lowerUrl.contains("doubleclick.net/pcs/click") ||
            lowerUrl.contains("googleadservices.com/pagead/aclk")
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

        // Dedicated YouTube Video & Feed Ad Interception (preserves real video playback streams)
        if (blockAds && (isYouTubeHost(reqHost) || isYouTubeHost(normalizedPageHost))) {
            if (isYouTubeAdRequest(reqHost, path, query)) {
                return true
            }
            // Never accidentally block essential YouTube media/thumbnail/API hosts
            if (isYouTubeCoreAsset(reqHost, path)) {
                return false
            }
        }

        // First-party resource: allow normal assets, block pure first-party ad/tracker/video-ad endpoints
        if (reqHost == normalizedPageHost) {
            if (blockAds) {
                if (FIRST_PARTY_AD_PATH_PATTERNS.any { path.contains(it) }) {
                    return true
                }
                if (VIDEO_AD_PATH_PATTERNS.any { path.contains(it) }) {
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

        // URL path & script heuristics for third-party ad, VAST/VPAID video-ad, and tracker resources
        if (blockAds) {
            if (THIRD_PARTY_AD_PATH_PATTERNS.any { path.contains(it) } ||
                VIDEO_AD_PATH_PATTERNS.any { path.contains(it) }
            ) {
                return true
            }
            if (query.contains("ad_type=") ||
                query.contains("adunit=") ||
                query.contains("adformat=") ||
                query.contains("vast_url=")
            ) {
                return true
            }
        }
        if (blockTrackers) {
            if (THIRD_PARTY_TRACKER_PATH_PATTERNS.any { path.contains(it) }) {
                return true
            }
            if ((query.contains("utm_source=") && path.contains("/collect")) ||
                (query.contains("fbclid=") && path.contains("pixel"))
            ) {
                return true
            }
        }
        return false
    }

    private fun isYouTubeAdRequest(reqHost: String, path: String, query: String): Boolean {
        if (reqHost == "ads.youtube.com" || reqHost == "adservice.google.com") return true
        if (reqHost.endsWith("youtube.com")) {
            if (YOUTUBE_AD_PATH_PATTERNS.any { path.contains(it) }) return true
            if (path.contains("/generate_204") && (query.contains("ad") || query.contains("adunit"))) return true
            if (path.contains("/get_video_info") && query.contains("adformat=")) return true
        }
        if (reqHost.endsWith("googlevideo.com")) {
            if (path.contains("/ptracking")) return true
            if (query.contains("adformat=") || query.contains("&oad=") || query.startsWith("oad=")) return true
        }
        return false
    }

    private fun isYouTubeCoreAsset(reqHost: String, path: String): Boolean {
        if (reqHost.endsWith("ytimg.com") || reqHost.endsWith("ggpht.com") || reqHost == "googlevideo.com" || reqHost.endsWith(".googlevideo.com")) {
            return true
        }
        if (reqHost.endsWith("youtube.com") && (
                path.startsWith("/youtubei/v1/player") ||
                    path.startsWith("/youtubei/v1/next") ||
                    path.startsWith("/youtubei/v1/browse") ||
                    path.startsWith("/youtubei/v1/search") ||
                    path.startsWith("/youtubei/v1/reel") ||
                    path.startsWith("/s/player/") ||
                    path.startsWith("/s/desktop/") ||
                    path.startsWith("/s/mobile/") ||
                    path.startsWith("/api/stats/watchtime") ||
                    path.startsWith("/api/stats/playback")
                )
        ) {
            return true
        }
        return false
    }

    /**
     * Returns a clean, resource-appropriate stub response so blocked scripts, VAST video-ad XML,
     * JSON ad APIs, stylesheets, and tracking pixels never cause JS exceptions or player hangs.
     */
    fun emptyBlockedResponse(requestUrl: String = ""): WebResourceResponse {
        val lowerUrl = requestUrl.lowercase()
        val lowerPath = runCatching { Uri.parse(requestUrl).path?.lowercase().orEmpty() }.getOrDefault("")
        val headers = mapOf(
            "X-SwiftShield" to "Blocked",
            "Access-Control-Allow-Origin" to "*"
        )
        return when {
            lowerPath.endsWith(".xml") || lowerPath.contains("/gampad/ads") || lowerUrl.contains("vast") -> WebResourceResponse(
                "application/xml",
                "utf-8",
                200,
                "OK",
                headers,
                ByteArrayInputStream("<VAST version=\"3.0\"></VAST>".toByteArray())
            )
            lowerPath.endsWith(".json") || lowerPath.contains("/youtubei/v1/") || lowerPath.contains("/api/") -> WebResourceResponse(
                "application/json",
                "utf-8",
                200,
                "OK",
                headers,
                ByteArrayInputStream("{}".toByteArray())
            )
            lowerPath.endsWith(".js") || lowerPath.endsWith(".mjs") -> WebResourceResponse(
                "application/javascript",
                "utf-8",
                200,
                "OK",
                headers,
                ByteArrayInputStream("/* SwiftShield */".toByteArray())
            )
            lowerPath.endsWith(".css") -> WebResourceResponse(
                "text/css",
                "utf-8",
                200,
                "OK",
                headers,
                ByteArrayInputStream(ByteArray(0))
            )
            lowerPath.endsWith(".gif") || lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".webp") -> WebResourceResponse(
                "image/gif",
                null,
                200,
                "OK",
                headers,
                ByteArrayInputStream(TRANSPARENT_1X1_GIF)
            )
            else -> WebResourceResponse(
                "text/plain",
                "utf-8",
                204,
                "No Content",
                headers,
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
            "ads", "ad", "adserver", "adservice", "adsystem", "adtech", "adclick",
            "pagead", "popads", "pubads", "securepubads", "googleads", "adx", "adsrv"
        )

        private val TRACKER_SUBDOMAIN_LABELS = setOf(
            "metrics", "telemetry", "beacon", "pixel", "track", "tracker", "tracking", "analytics"
        )

        private val YOUTUBE_AD_PATH_PATTERNS = listOf(
            "/pagead/", "/api/stats/ads", "/api/stats/atr", "/ptracking",
            "/get_midroll_info", "/youtubei/v1/log_event", "/youtubei/v1/ad_",
            "/ad_data_204", "/pcs/activeview", "/error_204", "/adunit/"
        )

        private val VIDEO_AD_PATH_PATTERNS = listOf(
            "/gampad/ads", "/gampad/live/ads", "/vast.xml", "/vast/", "/vpaid",
            "/ima3.js", "/ima3_debug.js", "/ads/vast", "/adtag", "/preroll",
            "/midroll", "/adbreak", "/ad/g/"
        )

        private val FIRST_PARTY_AD_PATH_PATTERNS = listOf(
            "/pagead/", "/adsense/", "/doubleclick/", "/adserver/", "/adsbygoogle",
            "/pubads_impl", "/gpt.js", "/prebid", "/ads/banner", "/sponsored-ads"
        )

        private val FIRST_PARTY_TRACKER_PATH_PATTERNS = listOf(
            "/analytics/collect", "/telemetry/v", "/pixel.gif", "/beacon.js", "/track/event"
        )

        private val THIRD_PARTY_AD_PATH_PATTERNS = listOf(
            "/pagead/", "/adsense/", "/doubleclick/", "/adserver/", "/adsbygoogle",
            "/pubads_impl", "/gpt.js", "/prebid", "/ads.js", "/ad_banner", "/sponsor_ad",
            "/amp-ad", "/amp-auto-ads", "/amp-sticky-ad", "/show_ads.js", "/ad_status.js"
        )

        private val THIRD_PARTY_TRACKER_PATH_PATTERNS = listOf(
            "/gtag/js", "/gtm.js", "/fbevents.js", "/analytics.js", "/pixel.gif",
            "/tr/?id=", "/collect?v=", "/beacon.min.js", "/matomo.js", "/piwik.js"
        )

        fun isYouTubeHost(hostOrUrl: String?): Boolean {
            if (hostOrUrl.isNullOrBlank()) return false
            val norm = normalizeHost(hostOrUrl)
            return norm == "youtube.com" ||
                norm.endsWith(".youtube.com") ||
                norm == "youtu.be" ||
                norm.endsWith(".youtu.be") ||
                norm == "youtube-nocookie.com" ||
                norm.endsWith(".youtube-nocookie.com")
        }

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

        val BASELINE_VIDEO_AD_DOMAINS = setOf(
            "imasdk.googleapis.com", "ads.youtube.com", "googleads.g.doubleclick.net",
            "pubads.g.doubleclick.net", "securepubads.g.doubleclick.net", "static.doubleclick.net",
            "s0.2mdn.net", "fwmrm.net", "freewheel.tv", "innovid.com", "spotxchange.com",
            "springserve.com", "vid.springserve.com", "teads.tv", "connatix.com", "primis.tech",
            "aniview.com", "stickyadstv.com", "tremorhub.com", "cedexis.com", "brightroll.com",
            "videologygroup.com", "tubemogul.com", "spotx.tv", "vidoomy.com", "brid.tv",
            "jwpltx.com", "adswizz.com", "tritondigital.com", "podscribe.com"
        )

        val BASELINE_AD_DOMAINS = setOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
            "pagead2.googlesyndication.com", "tpc.googlesyndication.com", "pagead2.googleadservices.com",
            "adnxs.com", "adsrvr.org", "advertising.com", "adform.net", "adroll.com",
            "amazon-adsystem.com", "criteo.com", "criteo.net", "outbrain.com", "taboola.com",
            "pubmatic.com", "rubiconproject.com", "openx.net", "casalemedia.com", "indexww.com",
            "smartadserver.com", "sharethrough.com", "triplelift.com", "yieldmo.com", "medianet.com",
            "media.net", "mgid.com", "revcontent.com", "bidswitch.net", "contextweb.com", "lijit.com",
            "sovrn.com", "zedo.com", "adcolony.com", "applovin.com", "chartboost.com", "inmobi.com",
            "mopub.com", "unityads.unity3d.com", "vungle.com", "ironsrc.com", "tapjoy.com",
            "flurry.com", "ads.yahoo.com", "adtech.de", "ads.twitter.com", "ads.linkedin.com",
            "ads.tiktok.com", "ads.pinterest.com", "ad.doubleclick.net", "adclick.g.doubleclick.net",
            "static.ads-twitter.com", "bat.bing.com", "ads.reddit.com", "ad-delivery.net",
            "adblade.com", "adbutler.com", "adhigh.net", "adition.com", "adkernel.com",
            "admanmedia.com", "admixer.net", "adnium.com", "adocean.pl", "adrecover.com",
            "adsafeprotected.com", "adscale.de", "adsco.re", "adskeeper.co.uk", "adsymptotic.com",
            "adtarget.io", "adthrive.com", "mediavine.com", "ezoic.net", "carbonads.net",
            "carbonads.com", "buysellads.com", "buysellads.net", "ethicalads.io", "serving-sys.com",
            "2mdn.net", "moatads.com", "doubleverify.com", "integralads.com", "3lift.com",
            "33across.com", "adlightning.com", "adpushup.com", "ads-twitter.com", "adsterra.com",
            "btloader.com", "emxdgt.com", "gumgum.com", "id5-sync.com", "kargo.com", "liveintent.com",
            "magnite.com", "nativo.com", "richaudience.com", "seedtag.com", "smilewanted.com",
            "sonobi.com", "undertone.com", "unruly.co", "yieldlove.com", "zemanta.com",
            "zergnet.com", "dianomi.com", "flashtalking.com", "sizmek.com", "celtra.com",
            "bidvertiser.com", "monetag.com", "galaksion.com", "evadav.com", "richpush.co",
            "pushground.com", "zeropark.com", "popmyads.com", "adspyglass.com", "yllix.com",
            "a-ads.com", "coinzilla.com", "cointraffic.io", "bitmedia.io", "prebid.org",
            "ad-maven.com", "rtmark.net", "acint.net", "adtrue.com", "adversal.com",
            "adxpremium.services", "anyclip.com", "avantisvideo.com", "bidgear.com", "clickio.com",
            "connectad.io", "deepintent.com", "districtm.io", "eskimi.com", "ex.co",
            "freestar.com", "improvedigital.com", "iqzone.com", "justpremium.com", "lkqd.net",
            "marfeel.com", "nitropay.com", "nobid.io", "onetag-sys.com", "playwire.com",
            "raptive.com", "rkdms.com", "setupad.com", "snigelweb.com", "Sortable.com".lowercase(),
            "stpd.cloud", "tappx.com", "thetradedesk.com", "unibots.in", "venatusmedia.com",
            "vlitag.com", "yahoo.com.ads", "yieldlab.net"
        )

        val BASELINE_POPUP_REDIRECT_DOMAINS = setOf(
            "popads.net", "popcash.net", "propellerads.com", "exoclick.com", "juicyads.com",
            "onclickads.net", "onclasrv.com", "adcash.com", "hilltopads.net", "clickadu.com",
            "admaven.com", "popunder.net", "trafficjunky.net", "trafficstars.com", "plugrush.com",
            "adsterra.network", "highcpmgate.com", "profitablegatecpm.com", "redirectvoluum.com",
            "trackvoluum.com", "pushame.com", "pushwoosh.com", "izooto.com", "subscribers.com",
            "notifpush.com", "alwingulla.com", "detikads.com", "revjet.com", "trafficforce.com",
            "ero-advertising.com", "adxpansion.com", "pushub.net", "pushwelcome.com",
            "onclickperformance.com", "onclickmax.com", "toprevenuegate.com", "effectivegatecpm.com",
            "highrevenuegate.com", "waytogrow.eu", "monetag.com", "realsrv.com", "magsrv.com"
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
            "medallia.com", "contentsquare.net", "quantummetric.com", "glassboxdigital.io", "dynatrace.com",
            "c.bing.com", "bat.r.msn.com", "analytics.twitter.com", "t.co/i/adsct", "b.scorecardresearch.com",
            "cdn.segment.com", "api.amplitude.com", "rs.fullstory.com", "cdn.mxpnl.com"
        )

        val BASELINE_CRYPTOMINER_DOMAINS = setOf(
            "coinhive.com", "coin-hive.com", "jsecoin.com", "cryptoloot.pro", "crypto-loot.com",
            "minero.cc", "webminepool.com", "authedmine.com", "monerominer.rocks", "ppoi.org",
            "webminerpool.com", "browsermine.com", "coinimp.com"
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
