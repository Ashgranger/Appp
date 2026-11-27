package com.example.engine

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import com.example.data.BrowserPreferencesState
import com.example.data.ImageLoadingPolicy
import com.example.viewmodel.ContextMenuPayload
import com.example.viewmodel.TabHistoryEntry
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

interface WebViewSessionListener {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, title: String, loadTimeMs: Long?, canGoBack: Boolean, canGoForward: Boolean)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleChanged(tabId: String, title: String)
    fun onTrackerBlocked(tabId: String, url: String)
    fun onTrackingParamsStripped(tabId: String, count: Int)
    fun onPopupOrRedirectBlocked(tabId: String, url: String)
    fun onPageError(tabId: String, description: String)
    fun onSslCertificateError(tabId: String, url: String, reason: String)
    fun onSafeBrowsingThreat(tabId: String, url: String, threatLabel: String)
    fun onFindResult(tabId: String, activeMatchOrdinal: Int, numberOfMatches: Int)
    fun onScrollDirectionChanged(scrollingDown: Boolean)
    fun onContextMenuRequested(payload: ContextMenuPayload)
    fun onDownloadRequested(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?)
    fun onFullscreenShow(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onFullscreenHide()
    fun onOpenNewTabUrl(url: String, isIncognito: Boolean)
    fun onLoginFormDetected(tabId: String, pageUrl: String)
    fun onLoginCredentialsCaptured(tabId: String, originUrl: String, username: String, password: String)
    fun onReaderEligibilityDetected(tabId: String, eligible: Boolean)
}

/**
 * Manages a high-speed pool of hardware-accelerated Android WebViews keyed by Tab ID.
 * Includes standby WebView pre-warming, renderer priority tuning, JS/CSS/image optimization,
 * thumbnail snapshotting, Print/PDF export, and Incognito session isolation.
 */
class WebViewSessionManager(
    private val context: Context,
    private val shieldEngine: SwiftShieldEngine,
    private val listener: WebViewSessionListener
) {
    private val pool = ConcurrentHashMap<String, WebView>()
    private val pageStartTimestamps = ConcurrentHashMap<String, Long>()
    private val currentHostByTab = ConcurrentHashMap<String, String>()
    private val earlyInjectedUrlByTab = ConcurrentHashMap<String, String>()
    private val incognitoVisitedHosts = ConcurrentHashMap.newKeySet<String>()
    private var prewarmedWebView: WebView? = null

    @Volatile
    private var currentPrefs: BrowserPreferencesState = BrowserPreferencesState()

    fun updatePreferences(prefs: BrowserPreferencesState) {
        currentPrefs = prefs
        pool.values.forEach { webView ->
            applySettingsToWebView(
                webView = webView,
                prefs = prefs,
                isIncognito = webView.tag == INCOGNITO_TAG,
                isDesktopMode = (webView.getTag(DESKTOP_TAG_KEY) as? Boolean) ?: prefs.defaultDesktopMode,
                textZoomPercent = (webView.getTag(ZOOM_TAG_KEY) as? Int) ?: prefs.defaultTextZoomPercent
            )
            webView.evaluateJavascript(
                RenderingOptimizerScripts.buildForceDarkScript(prefs.forceDarkWebPages),
                null
            )
        }
    }

    /**
     * Pre-warms a standby Chromium WebView instance on launch so first navigation feels instantaneous.
     */
    fun prewarm() {
        if (prewarmedWebView == null) {
            runCatching {
                prewarmedWebView = createRawWebView()
            }
        }
    }

    fun getOrCreateWebView(
        tabId: String,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        textZoomPercent: Int
    ): WebView {
        pool[tabId]?.let { existing ->
            return existing
        }

        val webView = if (prewarmedWebView != null) {
            prewarmedWebView!!.also { prewarmedWebView = null }
        } else {
            createRawWebView()
        }

        webView.tag = if (isIncognito) INCOGNITO_TAG else NORMAL_TAG
        webView.setTag(DESKTOP_TAG_KEY, isDesktopMode)
        webView.setTag(ZOOM_TAG_KEY, textZoomPercent)

        applySettingsToWebView(
            webView = webView,
            prefs = currentPrefs,
            isIncognito = isIncognito,
            isDesktopMode = isDesktopMode,
            textZoomPercent = textZoomPercent
        )

        attachClients(webView, tabId, isIncognito)
        pool[tabId] = webView
        return webView
    }

    fun getWebView(tabId: String): WebView? = pool[tabId]

    fun loadUrl(tabId: String, url: String, isIncognito: Boolean, isDesktopMode: Boolean, textZoomPercent: Int) {
        val webView = getOrCreateWebView(tabId, isIncognito, isDesktopMode, textZoomPercent)
        syncCookiePolicyForActiveTab(isIncognito)
        val normalizedHost = SwiftShieldEngine.normalizeHost(url)
        currentHostByTab[tabId] = normalizedHost
        if (isIncognito && normalizedHost.isNotBlank()) {
            incognitoVisitedHosts.add(normalizedHost)
        }
        pageStartTimestamps[tabId] = SystemClock.elapsedRealtime()
        webView.loadUrl(url)
    }

    fun syncCookiePolicyForActiveTab(isIncognito: Boolean) {
        runCatching {
            CookieManager.getInstance().setAcceptCookie(!isIncognito)
        }
    }

    fun reload(tabId: String) {
        pool[tabId]?.let {
            pageStartTimestamps[tabId] = SystemClock.elapsedRealtime()
            it.reload()
        }
    }

    fun stopLoading(tabId: String) {
        pool[tabId]?.stopLoading()
    }

    fun goBack(tabId: String): Boolean {
        val wv = pool[tabId] ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(tabId: String): Boolean {
        val wv = pool[tabId] ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun goToHistoryIndex(tabId: String, targetIndex: Int) {
        val wv = pool[tabId] ?: return
        val list = wv.copyBackForwardList()
        val steps = targetIndex - list.currentIndex
        if (steps != 0 && wv.canGoBackOrForward(steps)) {
            wv.goBackOrForward(steps)
        }
    }

    fun getTabHistory(tabId: String): List<TabHistoryEntry> {
        val wv = pool[tabId] ?: return emptyList()
        val list = wv.copyBackForwardList()
        val items = mutableListOf<TabHistoryEntry>()
        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)
            items.add(
                TabHistoryEntry(
                    index = i,
                    title = item.title?.takeIf { it.isNotBlank() } ?: item.url.orEmpty(),
                    url = item.url.orEmpty(),
                    isCurrent = i == list.currentIndex
                )
            )
        }
        return items.reversed()
    }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val wv = pool[tabId] ?: return
        wv.setTag(DESKTOP_TAG_KEY, enabled)
        applyUserAgentAndViewport(wv, enabled)
        if (!wv.url.isNullOrBlank()) {
            pageStartTimestamps[tabId] = SystemClock.elapsedRealtime()
            wv.reload()
        }
    }

    fun setTextZoom(tabId: String, zoomPercent: Int) {
        val wv = pool[tabId] ?: return
        val clamped = zoomPercent.coerceIn(50, 200)
        wv.setTag(ZOOM_TAG_KEY, clamped)
        wv.settings.textZoom = clamped
    }

    fun findInPage(tabId: String, query: String) {
        val wv = pool[tabId] ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            listener.onFindResult(tabId, 0, 0)
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        pool[tabId]?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        pool[tabId]?.clearMatches()
    }

    fun autofillCredentials(tabId: String, username: String, password: String, onDone: (Boolean) -> Unit = {}) {
        val wv = pool[tabId]
        if (wv == null) {
            onDone(false)
            return
        }
        wv.evaluateJavascript(
            RenderingOptimizerScripts.buildPasswordAutofillScript(username, password)
        ) { raw ->
            onDone(raw == "true")
        }
    }

    fun extractReaderArticle(tabId: String, onResult: (String?) -> Unit) {
        val wv = pool[tabId]
        if (wv == null) {
            onResult(null)
            return
        }
        wv.evaluateJavascript(RenderingOptimizerScripts.READER_EXTRACTOR_JS) { raw ->
            val unescaped = unquoteJsJson(raw)
            onResult(unescaped)
        }
    }

    fun extractPageSource(tabId: String, onResult: (String) -> Unit) {
        val wv = pool[tabId]
        if (wv == null) {
            onResult("")
            return
        }
        wv.evaluateJavascript("document.documentElement ? document.documentElement.outerHTML : ''") { raw ->
            val decoded = unquoteJsJson(raw).orEmpty()
            onResult(decoded)
        }
    }

    fun printCurrentPage(activityContext: Context, tabId: String, jobTitle: String) {
        val wv = pool[tabId] ?: return
        val printManager = activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = wv.createPrintDocumentAdapter(jobTitle.ifBlank { "Web Page" })
        printManager.print(
            "Browser - ${jobTitle.take(32)}",
            adapter,
            PrintAttributes.Builder().build()
        )
    }

    fun captureThumbnail(tabId: String): Bitmap? {
        val wv = pool[tabId] ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        return runCatching {
            val targetWidth = 320
            val scale = targetWidth.toFloat() / wv.width.toFloat()
            val targetHeight = (wv.height * scale).toInt().coerceIn(240, 520)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            canvas.translate(-wv.scrollX.toFloat(), -wv.scrollY.toFloat())
            wv.draw(canvas)
            bitmap
        }.getOrNull()
    }

    fun pauseInactiveTabs(activeTabId: String) {
        pool.forEach { (id, wv) ->
            if (id == activeTabId) {
                wv.onResume()
                wv.resumeTimers()
            } else {
                wv.onPause()
            }
        }
    }

    fun destroyTab(tabId: String) {
        currentHostByTab.remove(tabId)
        earlyInjectedUrlByTab.remove(tabId)
        pageStartTimestamps.remove(tabId)
        pool.remove(tabId)?.let { wv ->
            val wasIncognito = wv.tag == INCOGNITO_TAG
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            if (wasIncognito) {
                wv.clearCache(true)
                wv.clearFormData()
                wv.clearSslPreferences()
            }
            wv.clearHistory()
            wv.destroy()
        }
    }

    fun purgeIncognitoSessionData() {
        runCatching {
            val hostsToPurge = incognitoVisitedHosts.toList()
            incognitoVisitedHosts.clear()
            val storage = WebStorage.getInstance()
            hostsToPurge.forEach { host ->
                storage.deleteOrigin("https://$host")
                storage.deleteOrigin("http://$host")
            }
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
            @Suppress("DEPRECATION")
            WebViewDatabase.getInstance(context).clearFormData()
            WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
            pool.values.filter { it.tag == INCOGNITO_TAG }.forEach { wv ->
                wv.clearCache(true)
                wv.clearFormData()
                wv.clearHistory()
                wv.clearSslPreferences()
            }
            CookieManager.getInstance().setAcceptCookie(true)
        }
    }

    fun clearAllBrowserCacheAndCookies(clearCookies: Boolean = true) {
        pool.values.forEach { it.clearCache(true) }
        if (clearCookies) {
            runCatching {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
            }
        }
    }

    fun clearSiteDataForHost(host: String) {
        if (host.isBlank()) return
        runCatching {
            WebStorage.getInstance().deleteOrigin("https://$host")
            WebStorage.getInstance().deleteOrigin("http://$host")
            CookieManager.getInstance().removeSessionCookies(null)
            CookieManager.getInstance().flush()
        }
        pool.values.forEach { wv ->
            if (wv.url?.contains(host, ignoreCase = true) == true) {
                wv.clearCache(false)
                wv.reload()
            }
        }
    }

    fun destroyAll() {
        pool.keys.toList().forEach { destroyTab(it) }
        prewarmedWebView?.destroy()
        prewarmedWebView = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createRawWebView(): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Rely on activity hardware acceleration (LAYER_TYPE_NONE) so Android cleanly falls back on virtualized GPUs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            isFocusable = true
            isFocusableInTouchMode = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySettingsToWebView(
        webView: WebView,
        prefs: BrowserPreferencesState,
        isIncognito: Boolean,
        isDesktopMode: Boolean,
        textZoomPercent: Int
    ) {
        val s = webView.settings
        s.javaScriptEnabled = prefs.javaScriptEnabled
        s.javaScriptCanOpenWindowsAutomatically = false
        s.setSupportMultipleWindows(true)
        // Strict WebView security lockdown: block local filesystem & content provider access
        s.allowFileAccess = false
        s.allowContentAccess = false
        @Suppress("DEPRECATION")
        s.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        s.allowUniversalAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        s.saveFormData = false
        s.setGeolocationEnabled(false)

        // Strict Incognito storage isolation
        s.domStorageEnabled = !isIncognito
        @Suppress("DEPRECATION")
        s.databaseEnabled = !isIncognito
        s.cacheMode = when {
            isIncognito -> WebSettings.LOAD_NO_CACHE
            prefs.turboModeEnabled -> WebSettings.LOAD_DEFAULT
            else -> WebSettings.LOAD_DEFAULT
        }
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportZoom(true)
        s.textZoom = textZoomPercent.coerceIn(50, 200)
        // Block active mixed HTTP content inside HTTPS pages to prevent downgrade/MITM attacks
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.mediaPlaybackRequiresUserGesture = false

        // Image loading policy for speed / data saving
        when (prefs.imageLoadingPolicy) {
            ImageLoadingPolicy.ALWAYS, ImageLoadingPolicy.LAZY_OPTIMIZED -> {
                s.loadsImagesAutomatically = true
                s.blockNetworkImage = false
            }
            ImageLoadingPolicy.BLOCK_IMAGES -> {
                s.loadsImagesAutomatically = false
                s.blockNetworkImage = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.safeBrowsingEnabled = true
        }

        CookieManager.getInstance().setAcceptCookie(!isIncognito)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            !isIncognito && !prefs.blockThirdPartyCookies
        )

        applyUserAgentAndViewport(webView, isDesktopMode)
    }

    private fun applyUserAgentAndViewport(webView: WebView, isDesktopMode: Boolean) {
        val s = webView.settings
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
    }

    @SuppressLint("JavascriptInterface")
    private fun attachClients(webView: WebView, tabId: String, isIncognito: Boolean) {
        // Never attach the local password bridge inside Incognito tabs; require HTTPS origins
        if (!isIncognito) {
            webView.addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onPasswordFormDetected(pageUrl: String?) {
                        val safeUrl = pageUrl.orEmpty()
                        if (safeUrl.startsWith("https://", ignoreCase = true)) {
                            webView.post { listener.onLoginFormDetected(tabId, safeUrl) }
                        }
                    }

                    @JavascriptInterface
                    fun onCredentialsSubmitted(originUrl: String?, username: String?, password: String?) {
                        val safeUrl = originUrl.orEmpty()
                        val safePass = password.orEmpty()
                        if (safeUrl.startsWith("https://", ignoreCase = true) && safePass.length >= 2) {
                            webView.post {
                                listener.onLoginCredentialsCaptured(
                                    tabId = tabId,
                                    originUrl = safeUrl,
                                    username = username.orEmpty().trim(),
                                    password = safePass
                                )
                            }
                        }
                    }
                },
                PASSWORD_BRIDGE_NAME
            )
        }

        // Non-privileged shield telemetry bridge (safe in both Standard & Incognito tabs)
        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun onAdBlocked(source: String?) {
                    val safeSource = source?.take(64)?.ifBlank { "script-ad" } ?: "script-ad"
                    webView.post {
                        listener.onTrackerBlocked(tabId, "shield://$safeSource")
                    }
                }
            },
            SHIELD_BRIDGE_NAME
        )

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                listener.onFindResult(tabId, activeMatchOrdinal, numberOfMatches)
            }
        }

        var lastScrollY = 0
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val delta = scrollY - lastScrollY
            if (delta > 20 && scrollY > 64) {
                listener.onScrollDirectionChanged(true)
                lastScrollY = scrollY
            } else if (delta < -16 || scrollY <= 20) {
                listener.onScrollDirectionChanged(false)
                lastScrollY = scrollY
            }
        }

        webView.setOnLongClickListener {
            val hit = webView.hitTestResult
            val extra = hit.extra
            if (!extra.isNullOrBlank()) {
                when (hit.type) {
                    WebView.HitTestResult.SRC_ANCHOR_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        listener.onContextMenuRequested(ContextMenuPayload(url = extra, isImage = false))
                        return@setOnLongClickListener true
                    }
                    WebView.HitTestResult.IMAGE_TYPE -> {
                        listener.onContextMenuRequested(ContextMenuPayload(url = extra, isImage = true))
                        return@setOnLongClickListener true
                    }
                }
            }
            false
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            listener.onDownloadRequested(url, userAgent, contentDisposition, mimeType)
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                listener.onProgressChanged(tabId, newProgress)
                // Early DOM injection right as initial document scripts parse (critical for YouTube SPA hooks)
                val activeUrl = view?.url
                if (newProgress >= 15 && !activeUrl.isNullOrBlank() && activeUrl.startsWith("http")) {
                    if (earlyInjectedUrlByTab[tabId] != activeUrl) {
                        earlyInjectedUrlByTab[tabId] = activeUrl
                        injectShieldScripts(view, tabId, activeUrl)
                    }
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    listener.onTitleChanged(tabId, title)
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val pageHost = currentHostByTab[tabId]
                val blockPopups = currentPrefs.shieldEnabledGlobally &&
                    currentPrefs.blockPopupsAndRedirects &&
                    shieldEngine.isSiteProtected(pageHost)

                // Always block background / non-user-gesture scripted popups when popup blocker is active
                if (blockPopups && !isUserGesture) {
                    listener.onPopupOrRedirectBlocked(tabId, view?.url.orEmpty())
                    return false
                }

                val targetUrl = view?.hitTestResult?.extra
                if (!targetUrl.isNullOrBlank() && (targetUrl.startsWith("http://", true) || targetUrl.startsWith("https://", true))) {
                    if (blockPopups && shieldEngine.isPopupOrAdRedirect(targetUrl, pageHost)) {
                        listener.onPopupOrRedirectBlocked(tabId, targetUrl)
                        return false
                    }
                    val sanitized = shieldEngine.sanitizeTrackingParameters(targetUrl)
                    if (sanitized.strippedCount > 0) {
                        listener.onTrackingParamsStripped(tabId, sanitized.strippedCount)
                    }
                    listener.onOpenNewTabUrl(sanitized.cleanUrl, isIncognito)
                    return true
                }

                if (blockPopups) {
                    listener.onPopupOrRedirectBlocked(tabId, view?.url.orEmpty())
                }
                return false
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    listener.onFullscreenShow(view, callback)
                }
            }

            override fun onHideCustomView() {
                listener.onFullscreenHide()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val rawUrl = uri.toString()
                val scheme = uri.scheme?.lowercase().orEmpty()
                // Strictly allow only http/https navigation; block file://, content://, javascript:, intent://, data:
                if (scheme != "http" && scheme != "https") {
                    return true
                }

                if (request.isForMainFrame) {
                    val pageHost = currentHostByTab[tabId]
                    if (isIncognito) {
                        val destHost = SwiftShieldEngine.normalizeHost(rawUrl)
                        if (destHost.isNotBlank()) incognitoVisitedHosts.add(destHost)
                    }
                    // Block popunder / ad-redirect hijacks on protected sites
                    if (shieldEngine.isPopupOrAdRedirect(rawUrl, pageHost)) {
                        listener.onPopupOrRedirectBlocked(tabId, rawUrl)
                        return true
                    }

                    // Upgrade HTTP -> HTTPS if HTTPS-Only is enabled
                    var nextUrl = if (scheme == "http" && currentPrefs.httpsOnlyMode) {
                        uri.buildUpon().scheme("https").build().toString()
                    } else {
                        rawUrl
                    }

                    // Strip tracking query parameters (utm_*, fbclid, gclid, etc.)
                    val sanitized = shieldEngine.sanitizeTrackingParameters(nextUrl)
                    if (sanitized.strippedCount > 0) {
                        nextUrl = sanitized.cleanUrl
                        listener.onTrackingParamsStripped(tabId, sanitized.strippedCount)
                    }

                    if (nextUrl != rawUrl) {
                        view?.loadUrl(nextUrl)
                        return true
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val req = request ?: return null
                if (req.isForMainFrame) return null
                val url = req.url?.toString() ?: return null
                val pageHost = currentHostByTab[tabId]
                if (shieldEngine.shouldBlock(url, pageHost)) {
                    listener.onTrackerBlocked(tabId, url)
                    return shieldEngine.emptyBlockedResponse(url)
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (!url.isNullOrBlank()) {
                    currentHostByTab[tabId] = SwiftShieldEngine.normalizeHost(url)
                    earlyInjectedUrlByTab.remove(tabId)
                    pageStartTimestamps[tabId] = SystemClock.elapsedRealtime()
                    listener.onPageStarted(tabId, url)
                    if (url.startsWith("http")) {
                        injectShieldScripts(view, tabId, url)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val finalUrl = url.orEmpty()
                val startTime = pageStartTimestamps.remove(tabId)
                val loadTimeMs = startTime?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(1L) }

                // Inject speed, YouTube video-ad shield, cosmetic ad-hiding, and Force Dark optimizations cleanly
                if (view != null && finalUrl.startsWith("http")) {
                    val prefs = currentPrefs
                    val lazyImages = prefs.imageLoadingPolicy == ImageLoadingPolicy.LAZY_OPTIMIZED
                    if (lazyImages || prefs.cssPerfOptimizationEnabled || prefs.turboModeEnabled) {
                        view.evaluateJavascript(
                            RenderingOptimizerScripts.buildPerformanceScript(
                                lazyImages = lazyImages,
                                cssPerfOpt = prefs.cssPerfOptimizationEnabled,
                                turboMode = prefs.turboModeEnabled
                            ),
                            null
                        )
                    }
                    injectShieldScripts(view, tabId, finalUrl)
                    if (prefs.forceDarkWebPages) {
                        view.evaluateJavascript(RenderingOptimizerScripts.buildForceDarkScript(true), null)
                    }
                    if (prefs.passwordManagerEnabled) {
                        view.evaluateJavascript(RenderingOptimizerScripts.PASSWORD_FORM_OBSERVER_JS, null)
                    }
                    view.evaluateJavascript(RenderingOptimizerScripts.READER_ARTICLE_DETECT_JS) { raw ->
                        listener.onReaderEligibilityDetected(tabId, raw == "true")
                    }
                }

                listener.onPageFinished(
                    tabId = tabId,
                    url = finalUrl,
                    title = view?.title?.takeIf { it.isNotBlank() } ?: finalUrl,
                    loadTimeMs = loadTimeMs,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Unable to reach this page"
                    listener.onPageError(tabId, desc)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Fail-closed SSL certificate handling: NEVER call handler.proceed() on invalid certs
                handler?.cancel()
                val failingUrl = error?.url ?: view?.url.orEmpty()
                val reason = when (error?.primaryError) {
                    SslError.SSL_EXPIRED -> "This site's SSL security certificate has expired."
                    SslError.SSL_IDMISMATCH -> "The security certificate does not match this website's domain name."
                    SslError.SSL_UNTRUSTED -> "The certificate authority for this site is not trusted."
                    SslError.SSL_NOTYETVALID -> "This site's security certificate is not yet valid."
                    SslError.SSL_DATE_INVALID -> "The security certificate date is invalid."
                    else -> "This website presented an invalid or unsafe SSL/TLS certificate."
                }
                listener.onSslCertificateError(tabId, failingUrl, reason)
            }

            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                // Enforce Google Safe Browsing warning interstitial and threat reporting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    callback?.showInterstitial(true)
                }
                val threatLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    when (threatType) {
                        SAFE_BROWSING_THREAT_PHISHING -> "Deceptive / Phishing Site"
                        SAFE_BROWSING_THREAT_MALWARE -> "Malware Threat"
                        SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE -> "Unwanted Software"
                        SAFE_BROWSING_THREAT_BILLING -> "Potentially Harmful Billing Site"
                        else -> "Unsafe Website"
                    }
                } else {
                    "Unsafe Website"
                }
                listener.onSafeBrowsingThreat(tabId, request?.url?.toString().orEmpty(), threatLabel)
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                destroyTab(tabId)
                listener.onPageError(tabId, "Page renderer restarted. Tap reload to continue.")
                return true
            }
        }
    }

    private fun injectShieldScripts(view: WebView?, tabId: String, pageUrl: String) {
        if (view == null) return
        val prefs = currentPrefs
        val pageHost = currentHostByTab[tabId] ?: SwiftShieldEngine.normalizeHost(pageUrl)
        if (!prefs.shieldEnabledGlobally || !shieldEngine.isSiteProtected(pageHost)) return

        if (SwiftShieldEngine.isYouTubeHost(pageHost) && (prefs.blockAds || prefs.blockAnnoyances)) {
            val ytScript = RenderingOptimizerScripts.buildYouTubeShieldScript(
                hideAds = prefs.blockAds,
                hideAnnoyances = prefs.blockAnnoyances
            )
            if (ytScript.isNotEmpty()) {
                view.evaluateJavascript(ytScript, null)
            }
        }
        if (prefs.cosmeticBlockingEnabled) {
            val cosmeticScript = RenderingOptimizerScripts.buildCosmeticBlockingScript(
                hideAds = prefs.blockAds,
                hideAnnoyances = prefs.blockAnnoyances,
                blockPopups = prefs.blockPopupsAndRedirects
            )
            if (cosmeticScript.isNotEmpty()) {
                view.evaluateJavascript(cosmeticScript, null)
            }
        }
    }

    private fun unquoteJsJson(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching {
            JSONObject("{\"v\":$raw}").getString("v")
        }.getOrElse { raw.trim('"') }
    }

    companion object {
        private const val PASSWORD_BRIDGE_NAME = "SwiftPasswordBridge"
        private const val SHIELD_BRIDGE_NAME = "SwiftShieldBridge"
        private const val NORMAL_TAG = "normal"
        private const val INCOGNITO_TAG = "incognito"
        private const val DESKTOP_TAG_KEY = 0x5A010001
        private const val ZOOM_TAG_KEY = 0x5A010002

        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
