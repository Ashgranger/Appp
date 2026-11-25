package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "swiftweb_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black")
}

enum class OmniboxPosition(val label: String) {
    BOTTOM("Bottom (One-Handed)"),
    TOP("Top (Classic)")
}

enum class TabLayoutMode(val label: String) {
    GRID("Visual Grid"),
    LIST("Compact List")
}

enum class ImageLoadingPolicy(val label: String, val subtitle: String) {
    ALWAYS("Always Load", "Full fidelity images immediately"),
    LAZY_OPTIMIZED("Lazy & Optimized", "Defer offscreen images for faster loads"),
    BLOCK_IMAGES("Data Saver (Block)", "Skip images for maximum speed")
}

enum class FilterUpdateCadence(val label: String, val intervalMillis: Long) {
    DAILY("Daily", 24L * 60L * 60L * 1000L),
    WEEKLY("Weekly", 7L * 24L * 60L * 60L * 1000L),
    MANUAL("Manual Only", Long.MAX_VALUE)
}

enum class ReaderPaperTheme(val label: String) {
    LIGHT("Crisp Light"),
    SEPIA("Warm Sepia"),
    DARK("Slate Dark"),
    AMOLED("AMOLED")
}

enum class ReaderFontChoice(val label: String) {
    SANS("Sans"),
    SERIF("Serif"),
    MONO("Mono")
}

enum class ReaderLineSpacing(val label: String, val multiplier: Float) {
    COMPACT("Compact", 1.42f),
    BALANCED("Balanced", 1.65f),
    RELAXED("Relaxed", 1.92f)
}

data class BrowserPreferencesState(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val useDynamicColor: Boolean = false,
    val accentPreset: AccentPreset = AccentPreset.COBALT,
    val startPageShowHero: Boolean = true,
    val startPageShowStats: Boolean = true,
    val startPageShowMostVisited: Boolean = true,
    val dismissedMostVisitedHosts: Set<String> = emptySet(),
    val speedDialColumns: Int = 4,
    val customHomepageUrl: String = "",
    // Layout & Navigation
    val omniboxPosition: OmniboxPosition = OmniboxPosition.BOTTOM,
    val autoHideToolbarOnScroll: Boolean = true,
    val swipeTabsEnabled: Boolean = true,
    val swipeNavigateEnabled: Boolean = true,
    val tabLayoutMode: TabLayoutMode = TabLayoutMode.GRID,
    // Speed & Rendering Engine
    val turboModeEnabled: Boolean = true,
    val imageLoadingPolicy: ImageLoadingPolicy = ImageLoadingPolicy.LAZY_OPTIMIZED,
    val cssPerfOptimizationEnabled: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val defaultTextZoomPercent: Int = 100,
    val forceDarkWebPages: Boolean = false,
    val defaultDesktopMode: Boolean = false,
    // SwiftShield & Filter Lists
    val shieldEnabledGlobally: Boolean = true,
    val cosmeticBlockingEnabled: Boolean = true,
    val stripTrackingParamsEnabled: Boolean = true,
    val blockPopupsAndRedirects: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockSocial: Boolean = false,
    val blockAnnoyances: Boolean = true,
    val filterUpdateCadence: FilterUpdateCadence = FilterUpdateCadence.DAILY,
    val filterUpdateWifiOnly: Boolean = false,
    // Privacy & Search
    val httpsOnlyMode: Boolean = false,
    val blockThirdPartyCookies: Boolean = true,
    val clearHistoryOnExit: Boolean = false,
    val clearCacheOnExit: Boolean = false,
    val defaultSearchEngineId: String = "google",
    val searchSuggestionsEnabled: Boolean = true,
    // Local Encrypted Password Manager
    val passwordManagerEnabled: Boolean = true,
    val autoPromptSavePasswords: Boolean = true,
    // Reader Defaults
    val readerFontSizeSp: Int = 18,
    val readerPaperTheme: ReaderPaperTheme = ReaderPaperTheme.LIGHT,
    val readerFontChoice: ReaderFontChoice = ReaderFontChoice.SERIF,
    val readerLineSpacing: ReaderLineSpacing = ReaderLineSpacing.BALANCED,
    // Lifetime Telemetry
    val totalTrackersBlocked: Long = 0L
)

class BrowserPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val START_SHOW_HERO = booleanPreferencesKey("start_show_hero")
        val START_SHOW_STATS = booleanPreferencesKey("start_show_stats")
        val START_SHOW_MOST_VISITED = booleanPreferencesKey("start_show_most_visited")
        val DISMISSED_MOST_VISITED_HOSTS = stringSetPreferencesKey("dismissed_most_visited_hosts")
        val SPEED_DIAL_COLUMNS = intPreferencesKey("speed_dial_columns")
        val CUSTOM_HOMEPAGE = stringPreferencesKey("custom_homepage")

        val OMNIBOX_POSITION = stringPreferencesKey("omnibox_position")
        val AUTO_HIDE_TOOLBAR = booleanPreferencesKey("auto_hide_toolbar")
        val SWIPE_TABS = booleanPreferencesKey("swipe_tabs")
        val SWIPE_NAVIGATE = booleanPreferencesKey("swipe_navigate")
        val TAB_LAYOUT_MODE = stringPreferencesKey("tab_layout_mode")

        val TURBO_MODE = booleanPreferencesKey("turbo_mode")
        val IMAGE_POLICY = stringPreferencesKey("image_policy")
        val CSS_PERF_OPT = booleanPreferencesKey("css_perf_opt")
        val JS_ENABLED = booleanPreferencesKey("js_enabled")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val FORCE_DARK = booleanPreferencesKey("force_dark")
        val DEFAULT_DESKTOP = booleanPreferencesKey("default_desktop")

        val SHIELD_GLOBAL = booleanPreferencesKey("shield_global")
        val COSMETIC_BLOCK = booleanPreferencesKey("cosmetic_block")
        val STRIP_TRACKING_PARAMS = booleanPreferencesKey("strip_tracking_params")
        val BLOCK_POPUPS_REDIRECTS = booleanPreferencesKey("block_popups_redirects")
        val BLOCK_ADS = booleanPreferencesKey("block_ads")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val BLOCK_SOCIAL = booleanPreferencesKey("block_social")
        val BLOCK_ANNOYANCES = booleanPreferencesKey("block_annoyances")
        val FILTER_CADENCE = stringPreferencesKey("filter_cadence")
        val FILTER_WIFI_ONLY = booleanPreferencesKey("filter_wifi_only")

        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val BLOCK_3P_COOKIES = booleanPreferencesKey("block_3p_cookies")
        val CLEAR_HISTORY_EXIT = booleanPreferencesKey("clear_history_exit")
        val CLEAR_CACHE_EXIT = booleanPreferencesKey("clear_cache_exit")
        val DEFAULT_SEARCH_ENGINE = stringPreferencesKey("default_search_engine")
        val SEARCH_SUGGESTIONS_ENABLED = booleanPreferencesKey("search_suggestions_enabled")
        val PASSWORD_MANAGER_ENABLED = booleanPreferencesKey("password_manager_enabled")
        val AUTO_PROMPT_SAVE_PASSWORDS = booleanPreferencesKey("auto_prompt_save_passwords")

        val READER_FONT_SIZE = intPreferencesKey("reader_font_size")
        val READER_PAPER = stringPreferencesKey("reader_paper")
        val READER_FONT_CHOICE = stringPreferencesKey("reader_font_choice")
        val READER_LINE_SPACING = stringPreferencesKey("reader_line_spacing")
        val TOTAL_BLOCKED = longPreferencesKey("total_blocked")
    }

    val preferencesFlow: Flow<BrowserPreferencesState> = context.browserDataStore.data.map { p ->
        BrowserPreferencesState(
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AMOLED,
            useDynamicColor = p[Keys.USE_DYNAMIC_COLOR] ?: false,
            accentPreset = p[Keys.ACCENT_PRESET]?.let { runCatching { AccentPreset.valueOf(it) }.getOrNull() } ?: AccentPreset.COBALT,
            startPageShowHero = p[Keys.START_SHOW_HERO] ?: true,
            startPageShowStats = p[Keys.START_SHOW_STATS] ?: true,
            startPageShowMostVisited = p[Keys.START_SHOW_MOST_VISITED] ?: true,
            dismissedMostVisitedHosts = p[Keys.DISMISSED_MOST_VISITED_HOSTS] ?: emptySet(),
            speedDialColumns = (p[Keys.SPEED_DIAL_COLUMNS] ?: 4).coerceIn(3, 5),
            customHomepageUrl = p[Keys.CUSTOM_HOMEPAGE] ?: "",
            omniboxPosition = p[Keys.OMNIBOX_POSITION]?.let { runCatching { OmniboxPosition.valueOf(it) }.getOrNull() } ?: OmniboxPosition.BOTTOM,
            autoHideToolbarOnScroll = p[Keys.AUTO_HIDE_TOOLBAR] ?: true,
            swipeTabsEnabled = p[Keys.SWIPE_TABS] ?: true,
            swipeNavigateEnabled = p[Keys.SWIPE_NAVIGATE] ?: true,
            tabLayoutMode = p[Keys.TAB_LAYOUT_MODE]?.let { runCatching { TabLayoutMode.valueOf(it) }.getOrNull() } ?: TabLayoutMode.GRID,
            turboModeEnabled = p[Keys.TURBO_MODE] ?: true,
            imageLoadingPolicy = p[Keys.IMAGE_POLICY]?.let { runCatching { ImageLoadingPolicy.valueOf(it) }.getOrNull() } ?: ImageLoadingPolicy.LAZY_OPTIMIZED,
            cssPerfOptimizationEnabled = p[Keys.CSS_PERF_OPT] ?: true,
            javaScriptEnabled = p[Keys.JS_ENABLED] ?: true,
            defaultTextZoomPercent = (p[Keys.TEXT_ZOOM] ?: 100).coerceIn(50, 200),
            forceDarkWebPages = p[Keys.FORCE_DARK] ?: false,
            defaultDesktopMode = p[Keys.DEFAULT_DESKTOP] ?: false,
            shieldEnabledGlobally = p[Keys.SHIELD_GLOBAL] ?: true,
            cosmeticBlockingEnabled = p[Keys.COSMETIC_BLOCK] ?: true,
            stripTrackingParamsEnabled = p[Keys.STRIP_TRACKING_PARAMS] ?: true,
            blockPopupsAndRedirects = p[Keys.BLOCK_POPUPS_REDIRECTS] ?: true,
            blockAds = p[Keys.BLOCK_ADS] ?: true,
            blockTrackers = p[Keys.BLOCK_TRACKERS] ?: true,
            blockSocial = p[Keys.BLOCK_SOCIAL] ?: false,
            blockAnnoyances = p[Keys.BLOCK_ANNOYANCES] ?: true,
            filterUpdateCadence = p[Keys.FILTER_CADENCE]?.let { runCatching { FilterUpdateCadence.valueOf(it) }.getOrNull() } ?: FilterUpdateCadence.DAILY,
            filterUpdateWifiOnly = p[Keys.FILTER_WIFI_ONLY] ?: false,
            httpsOnlyMode = p[Keys.HTTPS_ONLY] ?: false,
            blockThirdPartyCookies = p[Keys.BLOCK_3P_COOKIES] ?: true,
            clearHistoryOnExit = p[Keys.CLEAR_HISTORY_EXIT] ?: false,
            clearCacheOnExit = p[Keys.CLEAR_CACHE_EXIT] ?: false,
            defaultSearchEngineId = p[Keys.DEFAULT_SEARCH_ENGINE] ?: "google",
            searchSuggestionsEnabled = p[Keys.SEARCH_SUGGESTIONS_ENABLED] ?: true,
            passwordManagerEnabled = p[Keys.PASSWORD_MANAGER_ENABLED] ?: true,
            autoPromptSavePasswords = p[Keys.AUTO_PROMPT_SAVE_PASSWORDS] ?: true,
            readerFontSizeSp = (p[Keys.READER_FONT_SIZE] ?: 18).coerceIn(14, 24),
            readerPaperTheme = p[Keys.READER_PAPER]?.let { runCatching { ReaderPaperTheme.valueOf(it) }.getOrNull() } ?: ReaderPaperTheme.LIGHT,
            readerFontChoice = p[Keys.READER_FONT_CHOICE]?.let { runCatching { ReaderFontChoice.valueOf(it) }.getOrNull() } ?: ReaderFontChoice.SERIF,
            readerLineSpacing = p[Keys.READER_LINE_SPACING]?.let { runCatching { ReaderLineSpacing.valueOf(it) }.getOrNull() } ?: ReaderLineSpacing.BALANCED,
            totalTrackersBlocked = p[Keys.TOTAL_BLOCKED] ?: 0L
        )
    }

    suspend fun update(block: (BrowserPreferencesState) -> BrowserPreferencesState) {
        context.browserDataStore.edit { p ->
            val current = BrowserPreferencesState(
                themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AMOLED,
                useDynamicColor = p[Keys.USE_DYNAMIC_COLOR] ?: false,
                accentPreset = p[Keys.ACCENT_PRESET]?.let { runCatching { AccentPreset.valueOf(it) }.getOrNull() } ?: AccentPreset.COBALT,
                startPageShowHero = p[Keys.START_SHOW_HERO] ?: true,
                startPageShowStats = p[Keys.START_SHOW_STATS] ?: true,
                startPageShowMostVisited = p[Keys.START_SHOW_MOST_VISITED] ?: true,
                dismissedMostVisitedHosts = p[Keys.DISMISSED_MOST_VISITED_HOSTS] ?: emptySet(),
                speedDialColumns = (p[Keys.SPEED_DIAL_COLUMNS] ?: 4).coerceIn(3, 5),
                customHomepageUrl = p[Keys.CUSTOM_HOMEPAGE] ?: "",
                omniboxPosition = p[Keys.OMNIBOX_POSITION]?.let { runCatching { OmniboxPosition.valueOf(it) }.getOrNull() } ?: OmniboxPosition.BOTTOM,
                autoHideToolbarOnScroll = p[Keys.AUTO_HIDE_TOOLBAR] ?: true,
                swipeTabsEnabled = p[Keys.SWIPE_TABS] ?: true,
                swipeNavigateEnabled = p[Keys.SWIPE_NAVIGATE] ?: true,
                tabLayoutMode = p[Keys.TAB_LAYOUT_MODE]?.let { runCatching { TabLayoutMode.valueOf(it) }.getOrNull() } ?: TabLayoutMode.GRID,
                turboModeEnabled = p[Keys.TURBO_MODE] ?: true,
                imageLoadingPolicy = p[Keys.IMAGE_POLICY]?.let { runCatching { ImageLoadingPolicy.valueOf(it) }.getOrNull() } ?: ImageLoadingPolicy.LAZY_OPTIMIZED,
                cssPerfOptimizationEnabled = p[Keys.CSS_PERF_OPT] ?: true,
                javaScriptEnabled = p[Keys.JS_ENABLED] ?: true,
                defaultTextZoomPercent = (p[Keys.TEXT_ZOOM] ?: 100).coerceIn(50, 200),
                forceDarkWebPages = p[Keys.FORCE_DARK] ?: false,
                defaultDesktopMode = p[Keys.DEFAULT_DESKTOP] ?: false,
                shieldEnabledGlobally = p[Keys.SHIELD_GLOBAL] ?: true,
                cosmeticBlockingEnabled = p[Keys.COSMETIC_BLOCK] ?: true,
                stripTrackingParamsEnabled = p[Keys.STRIP_TRACKING_PARAMS] ?: true,
                blockPopupsAndRedirects = p[Keys.BLOCK_POPUPS_REDIRECTS] ?: true,
                blockAds = p[Keys.BLOCK_ADS] ?: true,
                blockTrackers = p[Keys.BLOCK_TRACKERS] ?: true,
                blockSocial = p[Keys.BLOCK_SOCIAL] ?: false,
                blockAnnoyances = p[Keys.BLOCK_ANNOYANCES] ?: true,
                filterUpdateCadence = p[Keys.FILTER_CADENCE]?.let { runCatching { FilterUpdateCadence.valueOf(it) }.getOrNull() } ?: FilterUpdateCadence.DAILY,
                filterUpdateWifiOnly = p[Keys.FILTER_WIFI_ONLY] ?: false,
                httpsOnlyMode = p[Keys.HTTPS_ONLY] ?: false,
                blockThirdPartyCookies = p[Keys.BLOCK_3P_COOKIES] ?: true,
                clearHistoryOnExit = p[Keys.CLEAR_HISTORY_EXIT] ?: false,
                clearCacheOnExit = p[Keys.CLEAR_CACHE_EXIT] ?: false,
                defaultSearchEngineId = p[Keys.DEFAULT_SEARCH_ENGINE] ?: "google",
                searchSuggestionsEnabled = p[Keys.SEARCH_SUGGESTIONS_ENABLED] ?: true,
                passwordManagerEnabled = p[Keys.PASSWORD_MANAGER_ENABLED] ?: true,
                autoPromptSavePasswords = p[Keys.AUTO_PROMPT_SAVE_PASSWORDS] ?: true,
                readerFontSizeSp = (p[Keys.READER_FONT_SIZE] ?: 18).coerceIn(14, 24),
                readerPaperTheme = p[Keys.READER_PAPER]?.let { runCatching { ReaderPaperTheme.valueOf(it) }.getOrNull() } ?: ReaderPaperTheme.LIGHT,
                readerFontChoice = p[Keys.READER_FONT_CHOICE]?.let { runCatching { ReaderFontChoice.valueOf(it) }.getOrNull() } ?: ReaderFontChoice.SERIF,
                readerLineSpacing = p[Keys.READER_LINE_SPACING]?.let { runCatching { ReaderLineSpacing.valueOf(it) }.getOrNull() } ?: ReaderLineSpacing.BALANCED,
                totalTrackersBlocked = p[Keys.TOTAL_BLOCKED] ?: 0L
            )
            val updated = block(current)
            p[Keys.THEME_MODE] = updated.themeMode.name
            p[Keys.USE_DYNAMIC_COLOR] = updated.useDynamicColor
            p[Keys.ACCENT_PRESET] = updated.accentPreset.name
            p[Keys.START_SHOW_HERO] = updated.startPageShowHero
            p[Keys.START_SHOW_STATS] = updated.startPageShowStats
            p[Keys.START_SHOW_MOST_VISITED] = updated.startPageShowMostVisited
            p[Keys.DISMISSED_MOST_VISITED_HOSTS] = updated.dismissedMostVisitedHosts
            p[Keys.SPEED_DIAL_COLUMNS] = updated.speedDialColumns.coerceIn(3, 5)
            p[Keys.CUSTOM_HOMEPAGE] = updated.customHomepageUrl
            p[Keys.OMNIBOX_POSITION] = updated.omniboxPosition.name
            p[Keys.AUTO_HIDE_TOOLBAR] = updated.autoHideToolbarOnScroll
            p[Keys.SWIPE_TABS] = updated.swipeTabsEnabled
            p[Keys.SWIPE_NAVIGATE] = updated.swipeNavigateEnabled
            p[Keys.TAB_LAYOUT_MODE] = updated.tabLayoutMode.name
            p[Keys.TURBO_MODE] = updated.turboModeEnabled
            p[Keys.IMAGE_POLICY] = updated.imageLoadingPolicy.name
            p[Keys.CSS_PERF_OPT] = updated.cssPerfOptimizationEnabled
            p[Keys.JS_ENABLED] = updated.javaScriptEnabled
            p[Keys.TEXT_ZOOM] = updated.defaultTextZoomPercent.coerceIn(50, 200)
            p[Keys.FORCE_DARK] = updated.forceDarkWebPages
            p[Keys.DEFAULT_DESKTOP] = updated.defaultDesktopMode
            p[Keys.SHIELD_GLOBAL] = updated.shieldEnabledGlobally
            p[Keys.COSMETIC_BLOCK] = updated.cosmeticBlockingEnabled
            p[Keys.STRIP_TRACKING_PARAMS] = updated.stripTrackingParamsEnabled
            p[Keys.BLOCK_POPUPS_REDIRECTS] = updated.blockPopupsAndRedirects
            p[Keys.BLOCK_ADS] = updated.blockAds
            p[Keys.BLOCK_TRACKERS] = updated.blockTrackers
            p[Keys.BLOCK_SOCIAL] = updated.blockSocial
            p[Keys.BLOCK_ANNOYANCES] = updated.blockAnnoyances
            p[Keys.FILTER_CADENCE] = updated.filterUpdateCadence.name
            p[Keys.FILTER_WIFI_ONLY] = updated.filterUpdateWifiOnly
            p[Keys.HTTPS_ONLY] = updated.httpsOnlyMode
            p[Keys.BLOCK_3P_COOKIES] = updated.blockThirdPartyCookies
            p[Keys.CLEAR_HISTORY_EXIT] = updated.clearHistoryOnExit
            p[Keys.CLEAR_CACHE_EXIT] = updated.clearCacheOnExit
            p[Keys.DEFAULT_SEARCH_ENGINE] = updated.defaultSearchEngineId
            p[Keys.SEARCH_SUGGESTIONS_ENABLED] = updated.searchSuggestionsEnabled
            p[Keys.PASSWORD_MANAGER_ENABLED] = updated.passwordManagerEnabled
            p[Keys.AUTO_PROMPT_SAVE_PASSWORDS] = updated.autoPromptSavePasswords
            p[Keys.READER_FONT_SIZE] = updated.readerFontSizeSp.coerceIn(14, 24)
            p[Keys.READER_PAPER] = updated.readerPaperTheme.name
            p[Keys.READER_FONT_CHOICE] = updated.readerFontChoice.name
            p[Keys.READER_LINE_SPACING] = updated.readerLineSpacing.name
            p[Keys.TOTAL_BLOCKED] = updated.totalTrackersBlocked
        }
    }

    suspend fun incrementTrackersBlocked(delta: Long = 1L) {
        context.browserDataStore.edit { p ->
            val current = p[Keys.TOTAL_BLOCKED] ?: 0L
            p[Keys.TOTAL_BLOCKED] = current + delta
        }
    }
}
