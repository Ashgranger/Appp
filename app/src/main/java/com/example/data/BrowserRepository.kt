package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.engine.SwiftShieldEngine
import com.example.viewmodel.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

data class FilterUpdateSummary(
    val updatedSubscriptions: Int,
    val totalRulesLoaded: Int,
    val failedNames: List<String>
)

class BrowserRepository(
    private val context: Context,
    private val database: SwiftWebDatabase,
    val preferencesStore: BrowserPreferencesStore
) {
    private val bookmarkDao = database.bookmarkDao()
    private val historyDao = database.historyDao()
    private val downloadDao = database.downloadDao()
    private val filterDao = database.filterDao()
    private val allowlistDao = database.shieldAllowlistDao()
    private val customEngineDao = database.customSearchEngineDao()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .cookieJar(CookieJar.NO_COOKIES)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()
    }

    val preferencesFlow: Flow<BrowserPreferencesState> = preferencesStore.preferencesFlow
    val bookmarksFlow: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    val speedDialsFlow: Flow<List<BookmarkEntity>> = bookmarkDao.observeSpeedDials()
    val historyFlow: Flow<List<HistoryEntity>> = historyDao.observeRecent()
    val downloadsFlow: Flow<List<DownloadEntity>> = downloadDao.observeAll()
    val filterSubscriptionsFlow: Flow<List<FilterSubscriptionEntity>> = filterDao.observeSubscriptions()
    val activeBlockedDomainsFlow: Flow<List<String>> = filterDao.observeActiveBlockedDomains()
    val userCustomRulesFlow: Flow<List<FilterRuleEntity>> = filterDao.observeUserCustomRules()
    val allowlistFlow: Flow<List<ShieldAllowlistEntity>> = allowlistDao.observeAll()

    val allSearchEnginesFlow: Flow<List<SearchEngine>> = customEngineDao.observeAll().map { custom ->
        BUILT_IN_SEARCH_ENGINES + custom.map {
            SearchEngine(
                id = it.id,
                name = it.name,
                searchUrlTemplate = it.searchUrlTemplate,
                isCustom = true
            )
        }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val existingBookmarks = bookmarkDao.getAllOnce()
        val legacySeedUrls = setOf(
            "https://en.wikipedia.org",
            "https://duckduckgo.com",
            "https://github.com",
            "https://news.ycombinator.com",
            "https://developer.mozilla.org",
            "https://www.reddit.com"
        )
        val isUntouchedLegacySeed = existingBookmarks.size == legacySeedUrls.size &&
            existingBookmarks.all { it.isSpeedDial && it.url in legacySeedUrls }

        if (existingBookmarks.isEmpty() || isUntouchedLegacySeed) {
            if (isUntouchedLegacySeed) {
                existingBookmarks.forEach { bookmarkDao.deleteById(it.id) }
            }
            val now = System.currentTimeMillis()
            bookmarkDao.insertAll(
                listOf(
                    BookmarkEntity(title = "Google", url = "https://www.google.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 7000),
                    BookmarkEntity(title = "YouTube", url = "https://m.youtube.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 6000),
                    BookmarkEntity(title = "Wikipedia", url = "https://en.wikipedia.org", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 5000),
                    BookmarkEntity(title = "Amazon", url = "https://www.amazon.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 4000),
                    BookmarkEntity(title = "Instagram", url = "https://www.instagram.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 3000),
                    BookmarkEntity(title = "X", url = "https://x.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 2000),
                    BookmarkEntity(title = "Reddit", url = "https://www.reddit.com", folder = "Speed Dial", isSpeedDial = true, createdAt = now - 1000)
                )
            )
        }
        if (filterDao.subscriptionCount() == 0) {
            val now = System.currentTimeMillis()
            filterDao.insertSubscriptions(
                listOf(
                    FilterSubscriptionEntity(
                        id = "sub_ads_peterlowe",
                        name = "Peter Lowe's Ad & Tracking Server List",
                        category = "ADS",
                        sourceUrl = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
                        isEnabled = true,
                        ruleCount = SwiftShieldEngine.BASELINE_AD_DOMAINS.size,
                        lastUpdatedAt = now,
                        isBuiltIn = true
                    ),
                    FilterSubscriptionEntity(
                        id = "sub_trackers_stevenblack",
                        name = "Adware & Trackers Compact Hosts",
                        category = "TRACKERS",
                        sourceUrl = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                        isEnabled = true,
                        ruleCount = SwiftShieldEngine.BASELINE_TRACKER_DOMAINS.size,
                        lastUpdatedAt = now,
                        isBuiltIn = true
                    ),
                    FilterSubscriptionEntity(
                        id = "sub_annoyances_core",
                        name = "Mobile Annoyances & Cookie Banners",
                        category = "ANNOYANCES",
                        sourceUrl = "https://v.firebog.net/hosts/AdguardDNS.txt",
                        isEnabled = true,
                        ruleCount = SwiftShieldEngine.BASELINE_ANNOYANCE_DOMAINS.size,
                        lastUpdatedAt = now,
                        isBuiltIn = true
                    )
                )
            )
        }
    }

    // --- Bookmarks & Speed Dial ---
    suspend fun isBookmarked(url: String): Boolean = bookmarkDao.findByUrl(url) != null

    suspend fun upsertBookmark(title: String, url: String, folder: String = "General", isSpeedDial: Boolean = false) {
        val existing = bookmarkDao.findByUrl(url)
        if (existing != null) {
            bookmarkDao.update(existing.copy(title = title.ifBlank { url }, folder = folder, isSpeedDial = isSpeedDial))
        } else {
            bookmarkDao.upsert(BookmarkEntity(title = title.ifBlank { url }, url = url, folder = folder, isSpeedDial = isSpeedDial))
        }
    }

    suspend fun toggleSpeedDial(bookmark: BookmarkEntity) {
        bookmarkDao.update(bookmark.copy(isSpeedDial = !bookmark.isSpeedDial))
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)
    suspend fun deleteBookmarkByUrl(url: String) = bookmarkDao.deleteByUrl(url)

    // --- History ---
    suspend fun recordHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        historyDao.insert(HistoryEntity(title = title.ifBlank { url }, url = url))
    }

    suspend fun searchHistory(query: String): List<HistoryEntity> = historyDao.search(query)
    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)
    suspend fun clearHistorySince(sinceMillis: Long) = historyDao.deleteSince(sinceMillis)
    suspend fun clearAllHistory() = historyDao.clearAll()

    // --- Downloads ---
    suspend fun deleteDownloadRecord(id: Long) = downloadDao.deleteById(id)
    suspend fun clearDownloadRecords() = downloadDao.clearAll()

    // --- Shield Allowlist & Custom Rules ---
    suspend fun setSiteAllowed(host: String, allowed: Boolean) {
        val normalized = SwiftShieldEngine.normalizeHost(host)
        if (normalized.isBlank()) return
        if (allowed) {
            allowlistDao.allowHost(ShieldAllowlistEntity(host = normalized))
        } else {
            allowlistDao.removeHost(normalized)
        }
    }

    suspend fun addCustomBlockedDomain(domain: String) {
        val normalized = SwiftShieldEngine.normalizeHost(domain)
        if (normalized.contains('.')) {
            filterDao.insertRule(FilterRuleEntity(domain = normalized, subscriptionId = "user_custom", isUserCustom = true))
        }
    }

    suspend fun removeCustomBlockedDomain(domain: String) {
        filterDao.deleteUserCustomRule(domain)
    }

    suspend fun toggleFilterSubscription(id: String, enabled: Boolean) {
        filterDao.setSubscriptionEnabled(id, enabled)
    }

    suspend fun addCustomFilterSubscription(name: String, url: String): FilterSubscriptionEntity {
        val cleanUrl = if (url.startsWith("http://") || url.startsWith("https://")) url.trim() else "https://${url.trim()}"
        val entity = FilterSubscriptionEntity(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "Custom Filter List" },
            category = "CUSTOM",
            sourceUrl = cleanUrl,
            isEnabled = true,
            ruleCount = 0,
            lastUpdatedAt = 0L,
            isBuiltIn = false
        )
        filterDao.upsertSubscription(entity)
        return entity
    }

    suspend fun deleteCustomFilterSubscription(id: String) {
        filterDao.deleteRulesForSubscription(id)
        filterDao.deleteCustomSubscription(id)
    }

    /**
     * Fetches and parses all enabled filter list subscriptions from their live remote URLs,
     * refreshing the local Room database atomically.
     */
    suspend fun updateAllFilterLists(respectWifiOnly: Boolean, wifiOnlyEnabled: Boolean): FilterUpdateSummary =
        withContext(Dispatchers.IO) {
            if (respectWifiOnly && wifiOnlyEnabled && !isOnWifi()) {
                return@withContext FilterUpdateSummary(0, 0, listOf("Skipped (Wi-Fi only enabled)"))
            }
            val subs = filterDao.getSubscriptions().filter {
                it.isEnabled && it.sourceUrl.startsWith("https://", ignoreCase = true)
            }
            var updatedCount = 0
            var totalRules = 0
            val failed = mutableListOf<String>()

            for (sub in subs) {
                try {
                    val req = Request.Builder()
                        .url(sub.sourceUrl)
                        .header("User-Agent", "Browser-Shield/1.0")
                        .build()
                    httpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyText = response.body?.string().orEmpty()
                            val domains = SwiftShieldEngine.parseFilterListText(bodyText, maxRules = 12000)
                            if (domains.isNotEmpty()) {
                                filterDao.replaceSubscriptionRules(
                                    subscriptionId = sub.id,
                                    domains = domains,
                                    updatedAt = System.currentTimeMillis()
                                )
                                updatedCount++
                                totalRules += domains.size
                            } else {
                                failed.add(sub.name)
                            }
                        } else {
                            failed.add(sub.name)
                        }
                    }
                } catch (_: Exception) {
                    failed.add(sub.name)
                }
            }
            FilterUpdateSummary(updatedCount, totalRules, failed)
        }

    suspend fun shouldAutoUpdateFilters(prefs: BrowserPreferencesState): Boolean = withContext(Dispatchers.IO) {
        if (!prefs.shieldEnabledGlobally || prefs.filterUpdateCadence == FilterUpdateCadence.MANUAL) {
            return@withContext false
        }
        val subs = filterDao.getSubscriptions().filter { it.isEnabled }
        if (subs.isEmpty()) return@withContext false
        val oldestUpdate = subs.minOf { it.lastUpdatedAt }
        val elapsed = System.currentTimeMillis() - oldestUpdate
        elapsed >= prefs.filterUpdateCadence.intervalMillis
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // --- Custom Search Engines ---
    suspend fun addCustomSearchEngine(name: String, templateUrl: String): SearchEngine {
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        val normalizedTemplate = if (templateUrl.contains("{query}")) {
            templateUrl.trim()
        } else {
            "${templateUrl.trim()}{query}"
        }
        val fullUrl = if (normalizedTemplate.startsWith("http://") || normalizedTemplate.startsWith("https://")) {
            normalizedTemplate
        } else {
            "https://$normalizedTemplate"
        }
        customEngineDao.upsert(CustomSearchEngineEntity(id = id, name = name.trim(), searchUrlTemplate = fullUrl))
        return SearchEngine(id = id, name = name.trim(), searchUrlTemplate = fullUrl, isCustom = true)
    }

    suspend fun deleteCustomSearchEngine(id: String) = customEngineDao.deleteById(id)

    companion object {
        val BUILT_IN_SEARCH_ENGINES = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q={query}", false),
            SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}", false),
            SearchEngine("brave", "Brave Search", "https://search.brave.com/search?q={query}", false),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q={query}", false),
            SearchEngine("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search={query}", false)
        )
    }
}
