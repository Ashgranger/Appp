package com.example.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.example.data.BookmarkEntity
import com.example.data.BrowserPreferencesState
import com.example.data.DownloadEntity
import com.example.data.FeedbackEntry
import com.example.data.FilterRuleEntity
import com.example.data.FilterSubscriptionEntity
import com.example.data.HistoryEntity
import com.example.data.SavedCredential
import com.example.data.ShieldAllowlistEntity
import com.example.data.TabLayoutMode
import com.example.engine.NewsArticle
import com.example.engine.NewsCategory
import java.util.UUID

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrlTemplate: String,
    val isCustom: Boolean = false
) {
    fun buildQueryUrl(query: String): String {
        val encoded = android.net.Uri.encode(query.trim())
        return if (searchUrlTemplate.contains("{query}")) {
            searchUrlTemplate.replace("{query}", encoded)
        } else {
            "$searchUrlTemplate$encoded"
        }
    }
}

enum class TabGroupColor(val label: String, val color: Color) {
    COBALT("Cobalt", Color(0xFF3B82F6)),
    EMERALD("Emerald", Color(0xFF10B981)),
    AMBER("Amber", Color(0xFFF59E0B)),
    CRIMSON("Crimson", Color(0xFFF43F5E)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    TEAL("Teal", Color(0xFF06B6D4))
}

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: TabGroupColor = TabGroupColor.COBALT,
    val isCollapsed: Boolean = false,
    val isIncognito: Boolean = false
)

data class TabSession(
    val id: String = UUID.randomUUID().toString(),
    val orderIndex: Int = 0,
    val groupId: String? = null,
    val url: String = "",
    val title: String = "New Tab",
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val textZoomPercent: Int = 100,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val loadTimeMs: Long? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val blockedCount: Int = 0,
    val strippedParamsCount: Int = 0,
    val popupsBlockedCount: Int = 0,
    val hasError: Boolean = false,
    val errorDescription: String = "",
    val isReaderEligible: Boolean = false,
    val thumbnail: Bitmap? = null
) {
    val isStartPage: Boolean
        get() = url.isBlank()

    val displayHost: String
        get() = if (url.isBlank()) {
            if (isIncognito) "Incognito" else "Start Page"
        } else {
            runCatching { android.net.Uri.parse(url).host?.removePrefix("www.") }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: url
        }

    val isSecure: Boolean
        get() = url.startsWith("https://", ignoreCase = true)
}

data class ClosedTabRecord(
    val url: String,
    val title: String,
    val isIncognito: Boolean,
    val groupId: String?,
    val isDesktopMode: Boolean
)

enum class SuggestionKind {
    BOOKMARK,
    HISTORY,
    WEB_SEARCH
}

data class OmniboxSuggestion(
    val title: String,
    val url: String,
    val kind: SuggestionKind = SuggestionKind.HISTORY,
    val engineName: String = ""
) {
    val isBookmark: Boolean
        get() = kind == SuggestionKind.BOOKMARK
}

data class MostVisitedSite(
    val host: String,
    val url: String,
    val title: String,
    val visitCount: Int,
    val lastVisitedAt: Long
)

data class ReaderBlock(
    val kind: String, // heading, paragraph, quote, list_item, code
    val text: String
)

data class ReaderArticle(
    val title: String,
    val byline: String,
    val siteName: String,
    val url: String,
    val wordCount: Int,
    val blocks: List<ReaderBlock>,
    val publishedDate: String = "",
    val leadImageUrl: String = "",
    val strippedClutterCount: Int = 0
) {
    val readingMinutes: Int
        get() = maxOf(1, (wordCount + 199) / 200)
}

data class FindInPageState(
    val isActive: Boolean = false,
    val query: String = "",
    val activeMatchIndex: Int = 0,
    val totalMatches: Int = 0
)

data class ContextMenuPayload(
    val url: String,
    val isImage: Boolean
)

data class TabHistoryEntry(
    val index: Int,
    val title: String,
    val url: String,
    val isCurrent: Boolean
)

data class PendingCredentialPrompt(
    val host: String,
    val originUrl: String,
    val username: String,
    val password: String,
    val isUpdate: Boolean
)

enum class ActiveOverlay {
    NONE,
    OMNIBOX_SEARCH,
    TAB_SWITCHER,
    READER_VIEW,
    VIEW_SOURCE,
    LIBRARY,
    PASSWORDS,
    SETTINGS
}

enum class ActiveBottomSheet {
    NONE,
    TOOLS_MENU,
    SHIELD_PANEL,
    CONTEXT_MENU,
    TAB_HISTORY,
    SAVE_PASSWORD,
    FEEDBACK
}

enum class LibraryTab {
    BOOKMARKS,
    HISTORY,
    DOWNLOADS
}

enum class SettingsSection(val label: String) {
    APPEARANCE("Appearance"),
    LAYOUT("Layout & Tabs"),
    SPEED("Speed & Engine"),
    SHIELD("Shield & Privacy")
}

data class BrowserUiState(
    val tabs: List<TabSession> = emptyList(),
    val tabGroups: List<TabGroup> = emptyList(),
    val activeTabId: String = "",
    val preferences: BrowserPreferencesState = BrowserPreferencesState(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val speedDials: List<BookmarkEntity> = emptyList(),
    val mostVisitedSites: List<MostVisitedSite> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val downloads: List<DownloadEntity> = emptyList(),
    val downloadSpeedsById: Map<Long, Long> = emptyMap(),
    val filterSubscriptions: List<FilterSubscriptionEntity> = emptyList(),
    val userCustomFilterRules: List<FilterRuleEntity> = emptyList(),
    val allowlistedHosts: List<ShieldAllowlistEntity> = emptyList(),
    val searchEngines: List<SearchEngine> = emptyList(),
    // Encrypted Local Password Manager & Feedback
    val savedCredentials: List<SavedCredential> = emptyList(),
    val neverSavePasswordHosts: Set<String> = emptySet(),
    val pendingCredentialPrompt: PendingCredentialPrompt? = null,
    val autofillMatchesForActiveTab: List<SavedCredential> = emptyList(),
    val autofillBannerDismissedForHost: String? = null,
    val feedbackEntries: List<FeedbackEntry> = emptyList(),
    val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
    val activeSheet: ActiveBottomSheet = ActiveBottomSheet.NONE,
    val activeLibraryTab: LibraryTab = LibraryTab.BOOKMARKS,
    val activeSettingsSection: SettingsSection = SettingsSection.APPEARANCE,
    val omniboxQuery: String = "",
    val omniboxSuggestions: List<OmniboxSuggestion> = emptyList(),
    val isLoadingRemoteSuggestions: Boolean = false,
    val omniboxInstantAnswer: String? = null,
    val matchingOpenTabs: List<TabSession> = emptyList(),
    val findInPage: FindInPageState = FindInPageState(),
    val readerArticle: ReaderArticle? = null,
    val isExtractingReader: Boolean = false,
    val pageSourceCode: String = "",
    val isExtractingSource: Boolean = false,
    val contextMenuPayload: ContextMenuPayload? = null,
    val tabHistoryEntries: List<TabHistoryEntry> = emptyList(),
    // Tab Switcher Multi-Select & Segments
    val tabSwitcherIncognitoSegment: Boolean = false,
    val activeTabGroupFilterId: String? = null,
    val isTabMultiSelectMode: Boolean = false,
    val selectedTabIds: Set<String> = emptySet(),
    val tabLayoutOverride: TabLayoutMode? = null,
    // Toolbar auto-hide visibility
    val isChromeVisible: Boolean = true,
    // Filter list update status
    val isUpdatingFilterLists: Boolean = false,
    val activeRuleCount: Int = 0,
    // Live Start Page News Feed
    val selectedNewsCategory: NewsCategory = NewsCategory.TOP_STORIES,
    val newsArticles: List<NewsArticle> = emptyList(),
    val isLoadingNews: Boolean = false,
    val newsError: String? = null,
    // Snackbar / Undo messages
    val snackbarMessage: String? = null,
    val canUndoClosedTabs: Boolean = false,
    val canUndoDismissedMostVisited: Boolean = false
) {
    val activeTab: TabSession?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    val activeTabGroup: TabGroup?
        get() = activeTab?.groupId?.let { gid -> tabGroups.firstOrNull { it.id == gid } }

    val activeSearchEngine: SearchEngine
        get() = searchEngines.firstOrNull { it.id == preferences.defaultSearchEngineId }
            ?: searchEngines.firstOrNull()
            ?: SearchEngine("google", "Google", "https://www.google.com/search?q={query}")

    val normalTabs: List<TabSession>
        get() = tabs.filter { !it.isIncognito }.sortedBy { it.orderIndex }

    val incognitoTabs: List<TabSession>
        get() = tabs.filter { it.isIncognito }.sortedBy { it.orderIndex }

    val effectiveTabLayout: TabLayoutMode
        get() = tabLayoutOverride ?: preferences.tabLayoutMode
}
