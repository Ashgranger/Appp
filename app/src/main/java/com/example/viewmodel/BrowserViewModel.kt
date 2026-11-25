package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppAnalyticsTracker
import com.example.data.BookmarkEntity
import com.example.data.BrowserPreferencesState
import com.example.data.BrowserPreferencesStore
import com.example.data.BrowserRepository
import com.example.data.EncryptedPasswordStore
import com.example.data.FeedbackStore
import com.example.data.SavedCredential
import com.example.data.SwiftWebDatabase
import com.example.data.TabLayoutMode
import com.example.engine.NewsCategory
import com.example.engine.NewsFeedService
import com.example.engine.OmniboxMathEvaluator
import com.example.engine.SearchSuggestionService
import com.example.engine.SwiftDownloadEngine
import com.example.engine.SwiftShieldEngine
import com.example.engine.WebViewSessionListener
import com.example.engine.WebViewSessionManager
import com.example.ui.screens.isApkDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class BrowserViewModel(application: Application) : AndroidViewModel(application), WebViewSessionListener {

    private val database = SwiftWebDatabase.getInstance(application)
    private val prefsStore = BrowserPreferencesStore(application)
    private val repository = BrowserRepository(application, database, prefsStore)
    val passwordStore = EncryptedPasswordStore(application)
    val feedbackStore = FeedbackStore(application)
    val analyticsTracker = AppAnalyticsTracker(application)
    val shieldEngine = SwiftShieldEngine()
    val downloadEngine = SwiftDownloadEngine(application, database.downloadDao())
    val sessionManager = WebViewSessionManager(application, shieldEngine, this)
    private val searchSuggestionService = SearchSuggestionService()
    private val newsFeedService = NewsFeedService()

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    // Fullscreen video state (kept out of data class to avoid equals/hashCode overhead on View)
    private val _fullscreenCustomView = MutableStateFlow<Pair<View, WebChromeClient.CustomViewCallback>?>(null)
    val fullscreenCustomView: StateFlow<Pair<View, WebChromeClient.CustomViewCallback>?> = _fullscreenCustomView.asStateFlow()

    private val closedTabUndoBuffer = mutableListOf<ClosedTabRecord>()
    private var lastDismissedMostVisitedHost: String? = null
    private var autocompleteJob: Job? = null
    private var remoteSuggestJob: Job? = null
    private var newsFeedJob: Job? = null
    private var pendingBlockedIncrements = 0L
    private var blockedFlushJob: Job? = null

    init {
        val initialTab = TabSession(orderIndex = 0)
        _uiState.update {
            it.copy(
                tabs = listOf(initialTab),
                activeTabId = initialTab.id,
                searchEngines = BrowserRepository.BUILT_IN_SEARCH_ENGINES,
                activeRuleCount = shieldEngine.activeRuleCount()
            )
        }

        // Pre-warm a standby WebView for instant first navigation
        sessionManager.prewarm()

        viewModelScope.launch {
            repository.ensureSeeded()
            val prefs = repository.preferencesFlow.first()
            if (repository.shouldAutoUpdateFilters(prefs)) {
                launch { updateFilterListsNow(silent = true) }
            }
        }

        // Record privacy-safe app session (Firebase Analytics DAU/installs + local insights)
        analyticsTracker.recordAppOpen()

        observeLocalStores()
        refreshNewsFeed(forceRefresh = false)
    }

    fun selectNewsCategory(category: NewsCategory) {
        if (_uiState.value.selectedNewsCategory == category && _uiState.value.newsArticles.isNotEmpty()) {
            return
        }
        _uiState.update {
            it.copy(
                selectedNewsCategory = category,
                newsError = null
            )
        }
        refreshNewsFeed(forceRefresh = false)
    }

    fun refreshNewsFeed(forceRefresh: Boolean = true) {
        val category = _uiState.value.selectedNewsCategory
        newsFeedJob?.cancel()
        newsFeedJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNews = true, newsError = null) }
            val result = newsFeedService.fetchArticles(category = category, forceRefresh = forceRefresh)
            result.fold(
                onSuccess = { articles ->
                    _uiState.update {
                        if (it.selectedNewsCategory != category) it
                        else it.copy(
                            newsArticles = articles,
                            isLoadingNews = false,
                            newsError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        if (it.selectedNewsCategory != category) it
                        else it.copy(
                            isLoadingNews = false,
                            newsError = error.message ?: "Unable to load news headlines right now."
                        )
                    }
                }
            )
        }
    }

    private fun observeLocalStores() {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                shieldEngine.updatePreferences(prefs)
                sessionManager.updatePreferences(prefs)
                _uiState.update { state ->
                    val nextState = state.copy(
                        preferences = prefs,
                        activeRuleCount = shieldEngine.activeRuleCount(),
                        mostVisitedSites = computeMostVisitedSites(state.history, prefs.dismissedMostVisitedHosts)
                    )
                    nextState.copy(autofillMatchesForActiveTab = computeAutofillMatches(nextState))
                }
            }
        }
        viewModelScope.launch {
            repository.bookmarksFlow.collect { list ->
                _uiState.update { it.copy(bookmarks = list) }
            }
        }
        viewModelScope.launch {
            repository.speedDialsFlow.collect { list ->
                _uiState.update { it.copy(speedDials = list) }
            }
        }
        viewModelScope.launch {
            repository.historyFlow.collect { list ->
                _uiState.update { state ->
                    state.copy(
                        history = list,
                        mostVisitedSites = computeMostVisitedSites(list, state.preferences.dismissedMostVisitedHosts)
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.downloadsFlow.collect { list ->
                _uiState.update { it.copy(downloads = list) }
            }
        }
        viewModelScope.launch {
            downloadEngine.liveSpeedById.collect { speeds ->
                _uiState.update { it.copy(downloadSpeedsById = speeds) }
            }
        }
        viewModelScope.launch {
            repository.filterSubscriptionsFlow.collect { subs ->
                _uiState.update { it.copy(filterSubscriptions = subs) }
            }
        }
        viewModelScope.launch {
            repository.activeBlockedDomainsFlow.collect { domains ->
                shieldEngine.updateDynamicDomains(domains)
                _uiState.update { it.copy(activeRuleCount = shieldEngine.activeRuleCount()) }
            }
        }
        viewModelScope.launch {
            repository.userCustomRulesFlow.collect { rules ->
                _uiState.update { it.copy(userCustomFilterRules = rules) }
            }
        }
        viewModelScope.launch {
            repository.allowlistFlow.collect { hosts ->
                shieldEngine.updateAllowlist(hosts.map { it.host })
                _uiState.update { it.copy(allowlistedHosts = hosts) }
            }
        }
        viewModelScope.launch {
            repository.allSearchEnginesFlow.collect { engines ->
                _uiState.update { it.copy(searchEngines = engines) }
            }
        }
        viewModelScope.launch {
            passwordStore.credentialsFlow.collect { creds ->
                _uiState.update { state ->
                    val next = state.copy(savedCredentials = creds)
                    next.copy(autofillMatchesForActiveTab = computeAutofillMatches(next))
                }
            }
        }
        viewModelScope.launch {
            passwordStore.neverSaveHostsFlow.collect { hosts ->
                _uiState.update { it.copy(neverSavePasswordHosts = hosts) }
            }
        }
        viewModelScope.launch {
            feedbackStore.entriesFlow.collect { entries ->
                _uiState.update { it.copy(feedbackEntries = entries) }
            }
        }
    }

    // --- Navigation & Omnibox ---

    fun openOmnibox() {
        val activeUrl = _uiState.value.activeTab?.url.orEmpty()
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.OMNIBOX_SEARCH,
                activeSheet = ActiveBottomSheet.NONE,
                omniboxQuery = activeUrl,
                isChromeVisible = true
            )
        }
        refreshOmniboxSuggestions(activeUrl)
    }

    fun onOmniboxQueryChange(query: String, engineId: String? = null) {
        _uiState.update { it.copy(omniboxQuery = query) }
        refreshOmniboxSuggestions(query, engineId)
    }

    private fun refreshOmniboxSuggestions(rawQuery: String, overrideEngineId: String? = null) {
        autocompleteJob?.cancel()
        remoteSuggestJob?.cancel()
        val trimmed = rawQuery.trim()
        val instantMath = OmniboxMathEvaluator.evaluate(trimmed)
        if (trimmed.isEmpty()) {
            val topBookmarks = _uiState.value.bookmarks.take(5).map {
                OmniboxSuggestion(it.title, it.url, kind = SuggestionKind.BOOKMARK)
            }
            _uiState.update {
                it.copy(
                    omniboxSuggestions = topBookmarks,
                    isLoadingRemoteSuggestions = false,
                    omniboxInstantAnswer = null,
                    matchingOpenTabs = emptyList()
                )
            }
            return
        }

        autocompleteJob = viewModelScope.launch {
            delay(45)
            val state = _uiState.value
            val matchingTabs = state.tabs
                .filter { tab ->
                    tab.id != state.activeTabId &&
                        !tab.isStartPage &&
                        (tab.title.contains(trimmed, ignoreCase = true) ||
                            tab.url.contains(trimmed, ignoreCase = true))
                }
                .take(3)

            val bookmarkMatches = state.bookmarks.filter {
                it.title.contains(trimmed, ignoreCase = true) || it.url.contains(trimmed, ignoreCase = true)
            }.take(4).map { OmniboxSuggestion(it.title, it.url, kind = SuggestionKind.BOOKMARK) }

            val historyMatches = repository.searchHistory(trimmed)
                .distinctBy { it.url }
                .filter { h -> bookmarkMatches.none { it.url == h.url } }
                .take(4)
                .map { OmniboxSuggestion(it.title, it.url, kind = SuggestionKind.HISTORY) }

            val localMatches = bookmarkMatches + historyMatches
            val isIncognito = state.activeTab?.isIncognito == true
            val shouldFetchRemote = !isIncognito &&
                state.preferences.searchSuggestionsEnabled &&
                SearchSuggestionService.shouldFetchRemoteSuggestions(trimmed)

            _uiState.update {
                it.copy(
                    omniboxSuggestions = localMatches,
                    isLoadingRemoteSuggestions = shouldFetchRemote,
                    omniboxInstantAnswer = instantMath,
                    matchingOpenTabs = matchingTabs
                )
            }

            if (shouldFetchRemote) {
                val targetEngine = overrideEngineId?.let { id ->
                    state.searchEngines.firstOrNull { it.id == id }
                } ?: state.activeSearchEngine

                remoteSuggestJob = viewModelScope.launch {
                    delay(140)
                    val remoteTerms = searchSuggestionService.fetchSuggestions(
                        query = trimmed,
                        engineId = targetEngine.id,
                        maxResults = 6
                    )
                    // Ensure query hasn't changed while in-flight
                    if (_uiState.value.omniboxQuery.trim() != trimmed) return@launch

                    val localTitles = localMatches.map { it.title.lowercase() }.toSet()
                    val webSuggestions = remoteTerms
                        .filter { it.lowercase() !in localTitles }
                        .map { term ->
                            OmniboxSuggestion(
                                title = term,
                                url = "",
                                kind = SuggestionKind.WEB_SEARCH,
                                engineName = targetEngine.name
                            )
                        }

                    _uiState.update { current ->
                        if (current.omniboxQuery.trim() != trimmed) current
                        else current.copy(
                            omniboxSuggestions = localMatches + webSuggestions,
                            isLoadingRemoteSuggestions = false
                        )
                    }
                }
            }
        }
    }

    fun submitOmnibox(input: String, overrideEngineId: String? = null) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val resolvedUrl = resolveInputToUrl(trimmed, overrideEngineId)
        navigateActiveTab(resolvedUrl)
    }

    fun resolveInputToUrl(input: String, overrideEngineId: String? = null): String {
        val state = _uiState.value
        val trimmed = input.trim()
        val hasScheme = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        val looksLikeDomain = !trimmed.contains(" ") &&
            (trimmed.contains(".") || trimmed.startsWith("localhost", true))

        if (hasScheme) {
            return if (state.preferences.httpsOnlyMode && trimmed.startsWith("http://", true)) {
                "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
            } else {
                trimmed
            }
        }
        if (looksLikeDomain) {
            return "https://$trimmed"
        }
        val engine = if (overrideEngineId != null) {
            state.searchEngines.firstOrNull { it.id == overrideEngineId } ?: state.activeSearchEngine
        } else {
            state.activeSearchEngine
        }
        return engine.buildQueryUrl(trimmed)
    }

    fun navigateActiveTab(url: String) {
        val tab = _uiState.value.activeTab ?: return
        val sanitized = shieldEngine.sanitizeTrackingParameters(url)
        val targetUrl = sanitized.cleanUrl
        if (sanitized.strippedCount > 0) {
            recordBlockedTelemetry(sanitized.strippedCount.toLong())
        }
        _uiState.update { state ->
            val updated = state.copy(
                activeOverlay = ActiveOverlay.NONE,
                activeSheet = ActiveBottomSheet.NONE,
                isChromeVisible = true,
                findInPage = FindInPageState(),
                autofillBannerDismissedForHost = null,
                tabs = state.tabs.map {
                    if (it.id == tab.id) {
                        it.copy(
                            url = targetUrl,
                            isLoading = true,
                            progress = 10,
                            blockedCount = 0,
                            strippedParamsCount = sanitized.strippedCount,
                            popupsBlockedCount = 0,
                            hasError = false,
                            errorDescription = "",
                            isReaderEligible = false
                        )
                    } else it
                }
            )
            updated.copy(autofillMatchesForActiveTab = computeAutofillMatches(updated))
        }
        sessionManager.loadUrl(
            tabId = tab.id,
            url = targetUrl,
            isIncognito = tab.isIncognito,
            isDesktopMode = tab.isDesktopMode,
            textZoomPercent = tab.textZoomPercent
        )
    }

    fun goHome() {
        val tab = _uiState.value.activeTab ?: return
        val customHome = _uiState.value.preferences.customHomepageUrl.trim()
        if (customHome.isNotEmpty() && !tab.isIncognito) {
            navigateActiveTab(resolveInputToUrl(customHome))
            return
        }
        sessionManager.stopLoading(tab.id)
        _uiState.update { state ->
            state.copy(
                activeOverlay = ActiveOverlay.NONE,
                activeSheet = ActiveBottomSheet.NONE,
                isChromeVisible = true,
                autofillMatchesForActiveTab = emptyList(),
                autofillBannerDismissedForHost = null,
                tabs = state.tabs.map {
                    if (it.id == tab.id) {
                        it.copy(url = "", title = if (it.isIncognito) "Incognito" else "New Tab", isLoading = false, progress = 0, hasError = false, isReaderEligible = false)
                    } else it
                }
            )
        }
    }

    fun goBack(): Boolean {
        val tab = _uiState.value.activeTab ?: return false
        if (tab.isStartPage) return false
        if (sessionManager.goBack(tab.id)) {
            return true
        }
        // Return cleanly to Start Page when at the root of a tab's navigation stack
        goHome()
        return true
    }

    fun goForward() {
        val tab = _uiState.value.activeTab ?: return
        sessionManager.goForward(tab.id)
    }

    fun reloadOrStop() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) return
        if (tab.isLoading) {
            sessionManager.stopLoading(tab.id)
            updateTab(tab.id) { it.copy(isLoading = false, progress = 0) }
        } else {
            updateTab(tab.id) {
                it.copy(
                    hasError = false,
                    isLoading = true,
                    progress = 12,
                    blockedCount = 0,
                    strippedParamsCount = 0,
                    popupsBlockedCount = 0
                )
            }
            sessionManager.reload(tab.id)
        }
    }

    fun openTabHistorySheet() {
        val tab = _uiState.value.activeTab ?: return
        val historyItems = sessionManager.getTabHistory(tab.id)
        if (historyItems.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    tabHistoryEntries = historyItems,
                    activeSheet = ActiveBottomSheet.TAB_HISTORY
                )
            }
        }
    }

    fun navigateToTabHistoryIndex(index: Int) {
        val tab = _uiState.value.activeTab ?: return
        sessionManager.goToHistoryIndex(tab.id, index)
        _uiState.update { it.copy(activeSheet = ActiveBottomSheet.NONE) }
    }

    fun swipeSwitchTab(direction: Int) {
        val state = _uiState.value
        if (!state.preferences.swipeTabsEnabled) return
        val active = state.activeTab ?: return
        val siblings = state.tabs.filter { it.isIncognito == active.isIncognito }.sortedBy { it.orderIndex }
        if (siblings.size <= 1) return
        val currentIdx = siblings.indexOfFirst { it.id == active.id }
        if (currentIdx == -1) return
        val nextIdx = (currentIdx + direction).coerceIn(0, siblings.lastIndex)
        if (nextIdx != currentIdx) {
            selectTab(siblings[nextIdx].id)
        }
    }

    // --- Tab Management: Switcher, Groups, Drag-and-Drop Reorder, Multi-Select Bulk Close ---

    fun openTabSwitcher() {
        captureActiveTabThumbnail()
        val isIncognito = _uiState.value.activeTab?.isIncognito ?: false
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.TAB_SWITCHER,
                activeSheet = ActiveBottomSheet.NONE,
                tabSwitcherIncognitoSegment = isIncognito,
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet(),
                isChromeVisible = true
            )
        }
    }

    fun setTabSwitcherSegment(incognito: Boolean) {
        _uiState.update {
            it.copy(
                tabSwitcherIncognitoSegment = incognito,
                activeTabGroupFilterId = null,
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet()
            )
        }
    }

    fun setTabGroupFilter(groupId: String?) {
        _uiState.update { state ->
            val next = if (state.activeTabGroupFilterId == groupId) null else groupId
            state.copy(activeTabGroupFilterId = next)
        }
    }

    fun toggleTabLayoutMode() {
        val next = if (_uiState.value.effectiveTabLayout == TabLayoutMode.GRID) {
            TabLayoutMode.LIST
        } else {
            TabLayoutMode.GRID
        }
        _uiState.update { it.copy(tabLayoutOverride = next) }
        updatePreferences { it.copy(tabLayoutMode = next) }
    }

    fun createNewTab(
        url: String = "",
        isIncognito: Boolean = false,
        groupId: String? = null,
        switchToTab: Boolean = true
    ): TabSession {
        captureActiveTabThumbnail()
        val state = _uiState.value
        val nextIndex = (state.tabs.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val sanitized = if (url.isNotBlank()) shieldEngine.sanitizeTrackingParameters(url) else null
        val targetUrl = sanitized?.cleanUrl ?: url
        if ((sanitized?.strippedCount ?: 0) > 0) {
            recordBlockedTelemetry(sanitized!!.strippedCount.toLong())
        }
        val newTab = TabSession(
            orderIndex = nextIndex,
            groupId = groupId,
            url = targetUrl,
            title = if (targetUrl.isBlank()) (if (isIncognito) "Incognito" else "New Tab") else targetUrl,
            isIncognito = isIncognito,
            isDesktopMode = state.preferences.defaultDesktopMode,
            textZoomPercent = state.preferences.defaultTextZoomPercent,
            strippedParamsCount = sanitized?.strippedCount ?: 0,
            isLoading = targetUrl.isNotBlank()
        )
        _uiState.update { s ->
            s.copy(
                tabs = s.tabs + newTab,
                activeTabId = if (switchToTab) newTab.id else s.activeTabId,
                activeOverlay = if (switchToTab) ActiveOverlay.NONE else s.activeOverlay,
                activeSheet = ActiveBottomSheet.NONE,
                tabSwitcherIncognitoSegment = if (switchToTab) isIncognito else s.tabSwitcherIncognitoSegment,
                isChromeVisible = true
            )
        }
        if (targetUrl.isNotBlank()) {
            sessionManager.loadUrl(newTab.id, targetUrl, isIncognito, newTab.isDesktopMode, newTab.textZoomPercent)
        }
        if (switchToTab) {
            sessionManager.pauseInactiveTabs(newTab.id)
        }
        return newTab
    }

    fun selectTab(tabId: String) {
        captureActiveTabThumbnail()
        _uiState.update { state ->
            val updated = state.copy(
                activeTabId = tabId,
                activeOverlay = ActiveOverlay.NONE,
                activeSheet = ActiveBottomSheet.NONE,
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet(),
                findInPage = FindInPageState(),
                autofillBannerDismissedForHost = null,
                isChromeVisible = true
            )
            updated.copy(autofillMatchesForActiveTab = computeAutofillMatches(updated))
        }
        sessionManager.pauseInactiveTabs(tabId)
    }

    fun closeTab(tabId: String) {
        closeTabs(setOf(tabId))
    }

    fun closeTabs(tabIds: Set<String>) {
        if (tabIds.isEmpty()) return
        val state = _uiState.value
        val closing = state.tabs.filter { it.id in tabIds }
        if (closing.isEmpty()) return

        closedTabUndoBuffer.clear()
        closedTabUndoBuffer.addAll(
            closing.map {
                ClosedTabRecord(it.url, it.title, it.isIncognito, it.groupId, it.isDesktopMode)
            }
        )

        closing.forEach { sessionManager.destroyTab(it.id) }

        val remaining = state.tabs.filterNot { it.id in tabIds }
            .mapIndexed { idx, tab -> tab.copy(orderIndex = idx) }
            .toMutableList()

        // Purge Incognito session data if all incognito tabs are now closed
        val hadIncognito = closing.any { it.isIncognito }
        if (hadIncognito && remaining.none { it.isIncognito }) {
            sessionManager.purgeIncognitoSessionData()
        }

        // Always keep at least one normal tab alive
        if (remaining.isEmpty()) {
            remaining.add(
                TabSession(
                    orderIndex = 0,
                    isDesktopMode = state.preferences.defaultDesktopMode,
                    textZoomPercent = state.preferences.defaultTextZoomPercent
                )
            )
        }

        val newActiveId = if (state.activeTabId in tabIds) {
            val closedIncognito = closing.first().isIncognito
            remaining.lastOrNull { it.isIncognito == closedIncognito }?.id ?: remaining.last().id
        } else {
            state.activeTabId
        }

        // Clear group filter if the filtered group no longer has tabs
        val remainingGroupIds = remaining.mapNotNull { it.groupId }.toSet()
        val cleanedGroups = state.tabGroups.filter { it.id in remainingGroupIds }
        val nextFilterId = state.activeTabGroupFilterId?.takeIf { fid -> cleanedGroups.any { it.id == fid } }

        val label = if (closing.size == 1) "Closed tab" else "Closed ${closing.size} tabs"
        _uiState.update {
            it.copy(
                tabs = remaining,
                tabGroups = cleanedGroups,
                activeTabId = newActiveId,
                activeTabGroupFilterId = nextFilterId,
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet(),
                snackbarMessage = label,
                canUndoClosedTabs = true
            )
        }
        sessionManager.pauseInactiveTabs(newActiveId)
    }

    fun undoCloseTabs() {
        if (closedTabUndoBuffer.isEmpty()) return
        val toRestore = closedTabUndoBuffer.toList()
        closedTabUndoBuffer.clear()
        var lastRestoredId = _uiState.value.activeTabId
        toRestore.forEach { rec ->
            val restored = createNewTab(
                url = rec.url,
                isIncognito = rec.isIncognito,
                groupId = rec.groupId,
                switchToTab = false
            )
            lastRestoredId = restored.id
        }
        _uiState.update {
            it.copy(
                activeTabId = lastRestoredId,
                canUndoClosedTabs = false,
                snackbarMessage = null
            )
        }
    }

    fun reorderTab(fromTabId: String, toTabId: String) {
        if (fromTabId == toTabId) return
        _uiState.update { state ->
            val ordered = state.tabs.sortedBy { it.orderIndex }.toMutableList()
            val fromIndex = ordered.indexOfFirst { it.id == fromTabId }
            val toIndex = ordered.indexOfFirst { it.id == toTabId }
            if (fromIndex == -1 || toIndex == -1) return@update state
            val moved = ordered.removeAt(fromIndex)
            val targetGroupId = ordered.getOrNull(toIndex.coerceAtMost(ordered.lastIndex))?.groupId
            ordered.add(toIndex, moved.copy(groupId = targetGroupId ?: moved.groupId))
            state.copy(tabs = ordered.mapIndexed { idx, tab -> tab.copy(orderIndex = idx) })
        }
    }

    // --- Tab Groups ---

    fun createTabGroup(name: String, color: TabGroupColor, tabIds: Set<String>, isIncognito: Boolean) {
        val cleanName = name.trim().ifBlank { "Category" }
        val group = TabGroup(name = cleanName, color = color, isIncognito = isIncognito)
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups + group,
                tabs = state.tabs.map { if (it.id in tabIds) it.copy(groupId = group.id) else it },
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet(),
                snackbarMessage = "Created category \"$cleanName\"",
                canUndoClosedTabs = false
            )
        }
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        val groupName = groupId?.let { id -> _uiState.value.tabGroups.firstOrNull { it.id == id }?.name }
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) it.copy(groupId = groupId) else it },
                snackbarMessage = if (groupName != null) "Added tab to \"$groupName\"" else "Removed tab from category",
                canUndoClosedTabs = false
            )
        }
    }

    fun ungroupCategory(groupId: String) {
        val groupName = _uiState.value.tabGroups.firstOrNull { it.id == groupId }?.name ?: "category"
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups.filterNot { it.id == groupId },
                tabs = state.tabs.map { if (it.groupId == groupId) it.copy(groupId = null) else it },
                activeTabGroupFilterId = if (state.activeTabGroupFilterId == groupId) null else state.activeTabGroupFilterId,
                snackbarMessage = "Ungrouped \"$groupName\" (tabs kept open)",
                canUndoClosedTabs = false
            )
        }
    }

    fun renameAndRecolorGroup(groupId: String, newName: String, newColor: TabGroupColor) {
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups.map {
                    if (it.id == groupId) it.copy(name = newName.trim().ifBlank { it.name }, color = newColor) else it
                }
            )
        }
    }

    fun toggleGroupCollapsed(groupId: String) {
        _uiState.update { state ->
            state.copy(
                tabGroups = state.tabGroups.map {
                    if (it.id == groupId) it.copy(isCollapsed = !it.isCollapsed) else it
                }
            )
        }
    }

    fun closeTabGroup(groupId: String) {
        val ids = _uiState.value.tabs.filter { it.groupId == groupId }.map { it.id }.toSet()
        if (ids.isEmpty()) {
            ungroupCategory(groupId)
        } else {
            closeTabs(ids)
        }
    }

    // --- Multi-Select Bulk Tab Operations ---

    fun setTabMultiSelectMode(enabled: Boolean, initialTabId: String? = null) {
        _uiState.update {
            it.copy(
                isTabMultiSelectMode = enabled,
                selectedTabIds = if (enabled && initialTabId != null) setOf(initialTabId) else emptySet()
            )
        }
    }

    fun toggleTabSelected(tabId: String) {
        _uiState.update { state ->
            val next = state.selectedTabIds.toMutableSet()
            if (!next.add(tabId)) next.remove(tabId)
            state.copy(
                isTabMultiSelectMode = next.isNotEmpty() || state.isTabMultiSelectMode,
                selectedTabIds = next
            )
        }
    }

    fun selectAllTabsInCurrentSegment() {
        val state = _uiState.value
        val segmentIds = state.tabs.filter { it.isIncognito == state.tabSwitcherIncognitoSegment }.map { it.id }.toSet()
        val allSelected = segmentIds.isNotEmpty() && state.selectedTabIds.containsAll(segmentIds)
        _uiState.update {
            it.copy(
                isTabMultiSelectMode = true,
                selectedTabIds = if (allSelected) emptySet() else segmentIds
            )
        }
    }

    fun closeSelectedTabs() {
        closeTabs(_uiState.value.selectedTabIds)
    }

    fun closeOtherTabsInSegment() {
        val state = _uiState.value
        val keepId = state.activeTabId
        val toClose = state.tabs
            .filter { it.isIncognito == state.tabSwitcherIncognitoSegment && it.id != keepId }
            .map { it.id }
            .toSet()
        closeTabs(toClose)
    }

    fun closeAllTabsInSegment() {
        val state = _uiState.value
        val toClose = state.tabs
            .filter { it.isIncognito == state.tabSwitcherIncognitoSegment }
            .map { it.id }
            .toSet()
        closeTabs(toClose)
    }

    // --- Page Features: Desktop Mode, Zoom, Find-in-Page, Reader View, View Source, Print ---

    fun toggleDesktopModeForActiveTab() {
        val tab = _uiState.value.activeTab ?: return
        val next = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = next) }
        sessionManager.setDesktopMode(tab.id, next)
        showMessage(if (next) "Desktop site enabled" else "Mobile site enabled")
    }

    fun setZoomForActiveTab(zoomPercent: Int) {
        val tab = _uiState.value.activeTab ?: return
        val clamped = zoomPercent.coerceIn(50, 200)
        updateTab(tab.id) { it.copy(textZoomPercent = clamped) }
        sessionManager.setTextZoom(tab.id, clamped)
    }

    fun openFindInPage() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveBottomSheet.NONE,
                findInPage = FindInPageState(isActive = true),
                isChromeVisible = true
            )
        }
    }

    fun updateFindInPageQuery(query: String) {
        val tab = _uiState.value.activeTab ?: return
        _uiState.update { it.copy(findInPage = it.findInPage.copy(query = query)) }
        sessionManager.findInPage(tab.id, query)
    }

    fun findNextMatch(forward: Boolean) {
        val tab = _uiState.value.activeTab ?: return
        sessionManager.findNext(tab.id, forward)
    }

    fun closeFindInPage() {
        _uiState.value.activeTab?.let { sessionManager.clearFindMatches(it.id) }
        _uiState.update { it.copy(findInPage = FindInPageState()) }
    }

    fun openReaderView() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) {
            showMessage("Open an article page to use Reading Mode")
            return
        }
        _uiState.update {
            it.copy(
                activeSheet = ActiveBottomSheet.NONE,
                activeOverlay = ActiveOverlay.READER_VIEW,
                isExtractingReader = true,
                readerArticle = null
            )
        }
        sessionManager.extractReaderArticle(tab.id) { jsonStr ->
            val article = parseReaderJson(jsonStr, tab.url, tab.title)
            _uiState.update {
                it.copy(
                    isExtractingReader = false,
                    readerArticle = article
                )
            }
        }
    }

    fun openViewSource() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) return
        _uiState.update {
            it.copy(
                activeSheet = ActiveBottomSheet.NONE,
                activeOverlay = ActiveOverlay.VIEW_SOURCE,
                isExtractingSource = true,
                pageSourceCode = ""
            )
        }
        sessionManager.extractPageSource(tab.id) { html ->
            _uiState.update { it.copy(isExtractingSource = false, pageSourceCode = html) }
        }
    }

    fun printActivePage(activityContext: Context) {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) return
        _uiState.update { it.copy(activeSheet = ActiveBottomSheet.NONE) }
        sessionManager.printCurrentPage(activityContext, tab.id, tab.title)
    }

    /** Chrome/Safari-inspired one-tap web page translator */
    fun translateActivePage() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage || !tab.url.startsWith("http")) return
        val translatedUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(tab.url)}"
        navigateActiveTab(translatedUrl)
        showMessage("Translating page to English")
    }

    /** Arc/Brave-inspired one-tap mode switch between Standard and Incognito right from Start Page */
    fun switchBrowsingMode(incognito: Boolean) {
        val state = _uiState.value
        val existing = state.tabs.lastOrNull { it.isIncognito == incognito }
        if (existing != null) {
            selectTab(existing.id)
        } else {
            createNewTab(isIncognito = incognito, switchToTab = true)
        }
    }

    /** Vivaldi/Opera-inspired quick Search Engine cycler directly from the Start Page search pill */
    fun cycleSearchEngine() {
        val state = _uiState.value
        val engines = state.searchEngines
        if (engines.size <= 1) return
        val currentIdx = engines.indexOfFirst { it.id == state.activeSearchEngine.id }.coerceAtLeast(0)
        val nextEngine = engines[(currentIdx + 1) % engines.size]
        selectDefaultSearchEngine(nextEngine.id)
        showMessage("Search engine: ${nextEngine.name}")
    }

    /** DuckDuckGo-inspired one-tap Privacy Burn: closes all tabs and wipes history, cache, and cookies */
    fun burnSessionData() {
        val state = _uiState.value
        closedTabUndoBuffer.clear()
        state.tabs.forEach { sessionManager.destroyTab(it.id) }
        sessionManager.clearAllBrowserCacheAndCookies(clearCookies = true)
        sessionManager.purgeIncognitoSessionData()

        val freshTab = TabSession(
            orderIndex = 0,
            isDesktopMode = state.preferences.defaultDesktopMode,
            textZoomPercent = state.preferences.defaultTextZoomPercent
        )
        _uiState.update {
            it.copy(
                tabs = listOf(freshTab),
                tabGroups = emptyList(),
                activeTabId = freshTab.id,
                activeOverlay = ActiveOverlay.NONE,
                activeSheet = ActiveBottomSheet.NONE,
                findInPage = FindInPageState(),
                isTabMultiSelectMode = false,
                selectedTabIds = emptySet(),
                isChromeVisible = true,
                snackbarMessage = "Tabs and browsing data burned",
                canUndoClosedTabs = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllHistory()
        }
        sessionManager.prewarm()
    }

    // --- Bookmarks, Speed Dial, History, Downloads ---

    fun toggleBookmarkActiveTab(pinToSpeedDial: Boolean = false) {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) return
        viewModelScope.launch {
            val existing = _uiState.value.bookmarks.firstOrNull { it.url == tab.url }
            if (existing != null && !pinToSpeedDial) {
                repository.deleteBookmarkByUrl(tab.url)
                showMessage("Removed bookmark")
            } else {
                repository.upsertBookmark(
                    title = tab.title,
                    url = tab.url,
                    folder = if (pinToSpeedDial) "Speed Dial" else "General",
                    isSpeedDial = pinToSpeedDial || (existing?.isSpeedDial == true)
                )
                showMessage(if (pinToSpeedDial) "Pinned to Speed Dial" else "Saved to Bookmarks")
            }
        }
    }

    fun saveCustomBookmark(title: String, url: String, folder: String, isSpeedDial: Boolean) {
        val normalized = resolveInputToUrl(url)
        viewModelScope.launch {
            repository.upsertBookmark(
                title = title.ifBlank { normalized },
                url = normalized,
                folder = folder.ifBlank { "General" },
                isSpeedDial = isSpeedDial
            )
            showMessage(if (isSpeedDial) "Shortcut saved" else "Bookmark saved")
        }
    }

    fun toggleSpeedDialPin(bookmark: BookmarkEntity) {
        viewModelScope.launch { repository.toggleSpeedDial(bookmark) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    // --- Most Visited Sites Actions ---

    fun pinMostVisitedSite(site: MostVisitedSite) {
        viewModelScope.launch {
            repository.upsertBookmark(
                title = site.title.ifBlank { site.host },
                url = site.url,
                folder = "Speed Dial",
                isSpeedDial = true
            )
            showMessage("Pinned ${site.title.ifBlank { site.host }} to Favorites")
        }
    }

    fun dismissMostVisitedHost(host: String) {
        val norm = SwiftShieldEngine.normalizeHost(host)
        if (norm.isBlank()) return
        lastDismissedMostVisitedHost = norm
        viewModelScope.launch {
            repository.preferencesStore.update { prefs ->
                prefs.copy(dismissedMostVisitedHosts = prefs.dismissedMostVisitedHosts + norm)
            }
        }
        _uiState.update {
            it.copy(
                snackbarMessage = "Dismissed $norm from Most Visited",
                canUndoClosedTabs = false,
                canUndoDismissedMostVisited = true
            )
        }
    }

    fun undoDismissMostVisited() {
        val host = lastDismissedMostVisitedHost ?: return
        lastDismissedMostVisitedHost = null
        viewModelScope.launch {
            repository.preferencesStore.update { prefs ->
                prefs.copy(dismissedMostVisitedHosts = prefs.dismissedMostVisitedHosts - host)
            }
        }
        clearSnackbar()
    }

    fun restoreAllDismissedMostVisited() {
        lastDismissedMostVisitedHost = null
        viewModelScope.launch {
            repository.preferencesStore.update { prefs ->
                prefs.copy(dismissedMostVisitedHosts = emptySet())
            }
            showMessage("Restored all Most Visited sites")
        }
    }

    private fun computeMostVisitedSites(
        history: List<com.example.data.HistoryEntity>,
        dismissedHosts: Set<String>
    ): List<MostVisitedSite> {
        if (history.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<com.example.data.HistoryEntity>>()
        for (entry in history) {
            val host = SwiftShieldEngine.normalizeHost(entry.url)
            if (host.isBlank() || host in dismissedHosts) continue
            grouped.getOrPut(host) { mutableListOf() }.add(entry)
        }
        return grouped.entries.map { (host, visits) ->
            val latest = visits.maxByOrNull { it.visitedAt } ?: visits.first()
            val cleanTitle = latest.title
                .takeIf { it.isNotBlank() && !it.startsWith("http://", true) && !it.startsWith("https://", true) }
                ?: host
            MostVisitedSite(
                host = host,
                url = latest.url,
                title = cleanTitle,
                visitCount = visits.size,
                lastVisitedAt = latest.visitedAt
            )
        }
            .sortedWith(compareByDescending<MostVisitedSite> { it.visitCount }.thenByDescending { it.lastVisitedAt })
            .take(8)
    }

    fun clearBrowsingData(clearHistory: Boolean, clearCache: Boolean, clearCookies: Boolean, sinceMillis: Long = 0L) {
        viewModelScope.launch {
            if (clearHistory) {
                if (sinceMillis > 0L) repository.clearHistorySince(sinceMillis) else repository.clearAllHistory()
            }
            if (clearCache || clearCookies) {
                sessionManager.clearAllBrowserCacheAndCookies(clearCookies = clearCookies)
            }
            showMessage("Browsing data cleared")
        }
    }

    fun startManualDownload(url: String, mimeType: String? = null) {
        val resolved = resolveInputToUrl(url)
        downloadEngine.startDownload(
            url = resolved,
            mimeType = mimeType,
            onStarted = { fileName ->
                val isApk = isApkDownload(fileName, mimeType.orEmpty())
                analyticsTracker.recordDownloadStarted(fileName, mimeType.orEmpty(), isApk)
                showMessage("Downloading $fileName")
            }
        )
    }

    fun pauseDownload(id: Long) {
        downloadEngine.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        downloadEngine.resumeDownload(id)
    }

    fun cancelDownload(id: Long) {
        downloadEngine.cancelDownload(id)
    }

    fun deleteDownloadRecord(id: Long) {
        downloadEngine.deleteDownload(id)
    }

    fun clearDownloadRecords() {
        downloadEngine.clearFinishedDownloads()
    }

    fun openDownloadedFile(activityContext: Context, item: com.example.data.DownloadEntity) {
        val opened = downloadEngine.openDownloadedFile(activityContext, item)
        if (!opened) {
            showMessage("File not found on device or no viewer available")
        }
    }

    fun shareDownloadedFile(activityContext: Context, item: com.example.data.DownloadEntity) {
        val shared = downloadEngine.shareDownloadedFile(activityContext, item)
        if (!shared) {
            showMessage("File unavailable to share")
        }
    }

    // --- SwiftShield, Filter Lists & Custom Search Engines ---

    fun toggleProtectionForCurrentSite() {
        val host = _uiState.value.activeTab?.displayHost ?: return
        if (host == "Start Page" || host == "Incognito") return
        val currentlyProtected = shieldEngine.isSiteProtected(host)
        viewModelScope.launch {
            repository.setSiteAllowed(host, allowed = currentlyProtected)
            _uiState.value.activeTab?.id?.let { sessionManager.reload(it) }
            showMessage(if (currentlyProtected) "SwiftShield paused for $host" else "SwiftShield enabled for $host")
        }
    }

    fun removeAllowlistedHost(host: String) {
        viewModelScope.launch { repository.setSiteAllowed(host, allowed = false) }
    }

    fun clearDataForCurrentSite() {
        val host = _uiState.value.activeTab?.displayHost ?: return
        sessionManager.clearSiteDataForHost(host)
        showMessage("Cleared site data for $host")
    }

    fun toggleFilterSubscription(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.toggleFilterSubscription(id, enabled) }
    }

    fun addCustomFilterSubscription(name: String, url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            repository.addCustomFilterSubscription(name, url)
            showMessage("Filter list added — updating...")
            updateFilterListsNow(silent = false)
        }
    }

    fun deleteCustomFilterSubscription(id: String) {
        viewModelScope.launch { repository.deleteCustomFilterSubscription(id) }
    }

    fun addCustomBlockedDomain(domain: String) {
        viewModelScope.launch {
            repository.addCustomBlockedDomain(domain)
            showMessage("Added custom block rule")
        }
    }

    fun removeCustomBlockedDomain(domain: String) {
        viewModelScope.launch { repository.removeCustomBlockedDomain(domain) }
    }

    fun updateFilterListsNow(silent: Boolean = false) {
        if (_uiState.value.isUpdatingFilterLists) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingFilterLists = true) }
            val prefs = _uiState.value.preferences
            val summary = repository.updateAllFilterLists(
                respectWifiOnly = silent,
                wifiOnlyEnabled = prefs.filterUpdateWifiOnly
            )
            _uiState.update {
                it.copy(
                    isUpdatingFilterLists = false,
                    activeRuleCount = shieldEngine.activeRuleCount()
                )
            }
            if (!silent) {
                val msg = if (summary.updatedSubscriptions > 0) {
                    "Updated ${summary.updatedSubscriptions} lists (${summary.totalRulesLoaded} rules)"
                } else {
                    "Filter lists offline — using ${shieldEngine.activeRuleCount()} built-in rules"
                }
                showMessage(msg)
            }
        }
    }

    fun selectDefaultSearchEngine(engineId: String) {
        updatePreferences { it.copy(defaultSearchEngineId = engineId) }
    }

    fun addCustomSearchEngine(name: String, templateUrl: String) {
        if (name.isBlank() || templateUrl.isBlank()) return
        viewModelScope.launch {
            val engine = repository.addCustomSearchEngine(name, templateUrl)
            updatePreferences { it.copy(defaultSearchEngineId = engine.id) }
            showMessage("Added ${engine.name} search engine")
        }
    }

    fun deleteCustomSearchEngine(id: String) {
        viewModelScope.launch {
            repository.deleteCustomSearchEngine(id)
            if (_uiState.value.preferences.defaultSearchEngineId == id) {
                updatePreferences { it.copy(defaultSearchEngineId = "google") }
            }
        }
    }

    // --- Overlays, Sheets & Preferences ---

    fun setOverlay(overlay: ActiveOverlay) {
        _uiState.update {
            it.copy(
                activeOverlay = overlay,
                activeSheet = ActiveBottomSheet.NONE,
                isChromeVisible = true
            )
        }
    }

    fun openLibrary(tab: LibraryTab = LibraryTab.BOOKMARKS) {
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.LIBRARY,
                activeSheet = ActiveBottomSheet.NONE,
                activeLibraryTab = tab
            )
        }
    }

    fun openSettings(section: SettingsSection = SettingsSection.APPEARANCE) {
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.SETTINGS,
                activeSheet = ActiveBottomSheet.NONE,
                activeSettingsSection = section
            )
        }
    }

    fun setSettingsSection(section: SettingsSection) {
        _uiState.update { it.copy(activeSettingsSection = section) }
    }

    fun setLibraryTab(tab: LibraryTab) {
        _uiState.update { it.copy(activeLibraryTab = tab) }
    }

    fun setBottomSheet(sheet: ActiveBottomSheet) {
        _uiState.update { it.copy(activeSheet = sheet, isChromeVisible = true) }
    }

    // --- Local Encrypted Password Manager & Quick Feedback ---

    fun openPasswordVault() {
        _uiState.update {
            it.copy(
                activeOverlay = ActiveOverlay.PASSWORDS,
                activeSheet = ActiveBottomSheet.NONE,
                isChromeVisible = true
            )
        }
    }

    fun openFeedbackSheet() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveBottomSheet.FEEDBACK,
                isChromeVisible = true
            )
        }
    }

    fun submitFeedback(rating: Int, category: String, message: String) {
        feedbackStore.submit(rating, category, message)
        _uiState.update { it.copy(activeSheet = ActiveBottomSheet.NONE) }
        showMessage("Thanks for your feedback!")
    }

    fun savePendingCredential(username: String, password: String) {
        val pending = _uiState.value.pendingCredentialPrompt ?: return
        val cleanPass = password.trim()
        if (cleanPass.isEmpty()) return
        passwordStore.upsertCredential(
            hostOrUrl = pending.host,
            originUrl = pending.originUrl,
            username = username.trim(),
            password = cleanPass
        )
        analyticsTracker.recordPasswordSaved()
        _uiState.update {
            it.copy(
                pendingCredentialPrompt = null,
                activeSheet = ActiveBottomSheet.NONE
            )
        }
        showMessage(if (pending.isUpdate) "Updated password for ${pending.host}" else "Saved encrypted password for ${pending.host}")
    }

    fun neverSaveForPendingHost() {
        val pending = _uiState.value.pendingCredentialPrompt ?: return
        passwordStore.addNeverSaveHost(pending.host)
        _uiState.update {
            it.copy(
                pendingCredentialPrompt = null,
                activeSheet = ActiveBottomSheet.NONE
            )
        }
        showMessage("Won't ask to save passwords for ${pending.host}")
    }

    fun dismissSavePasswordPrompt() {
        _uiState.update {
            it.copy(
                pendingCredentialPrompt = null,
                activeSheet = if (it.activeSheet == ActiveBottomSheet.SAVE_PASSWORD) ActiveBottomSheet.NONE else it.activeSheet
            )
        }
    }

    fun autofillActiveTab(credential: SavedCredential) {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isStartPage) return
        sessionManager.autofillCredentials(tab.id, credential.username, credential.password) { filled ->
            if (filled) {
                analyticsTracker.recordPasswordAutofilled()
                showMessage("Auto-filled ${credential.username.ifBlank { credential.host }}")
            } else {
                showMessage("Open a login form on ${tab.displayHost} to auto-fill")
            }
        }
        _uiState.update { it.copy(autofillBannerDismissedForHost = tab.displayHost) }
    }

    fun dismissAutofillBanner() {
        val host = _uiState.value.activeTab?.displayHost ?: return
        _uiState.update { it.copy(autofillBannerDismissedForHost = host) }
    }

    fun saveManualCredential(hostOrUrl: String, username: String, password: String, existingId: String? = null) {
        val normHost = SwiftShieldEngine.normalizeHost(hostOrUrl).ifBlank { hostOrUrl.trim().lowercase() }
        if (normHost.isBlank() || password.isBlank()) return
        passwordStore.upsertCredential(
            hostOrUrl = normHost,
            originUrl = if (hostOrUrl.startsWith("http", true)) hostOrUrl else "https://$normHost",
            username = username.trim(),
            password = password,
            existingId = existingId
        )
        analyticsTracker.recordPasswordSaved()
        showMessage(if (existingId != null) "Updated login for $normHost" else "Saved encrypted login for $normHost")
    }

    fun deleteCredential(id: String) {
        passwordStore.deleteCredential(id)
        showMessage("Deleted saved password")
    }

    fun removeNeverSavePasswordHost(host: String) {
        passwordStore.removeNeverSaveHost(host)
    }

    fun clearAllSavedCredentials() {
        passwordStore.clearAllCredentials()
        showMessage("Cleared all saved passwords")
    }

    private fun computeAutofillMatches(state: BrowserUiState): List<SavedCredential> {
        if (!state.preferences.passwordManagerEnabled) return emptyList()
        val tab = state.activeTab ?: return emptyList()
        if (tab.isStartPage || !tab.url.startsWith("http", true)) return emptyList()
        val host = tab.displayHost
        if (host.isBlank() || host == state.autofillBannerDismissedForHost) return emptyList()
        return passwordStore.findForHost(tab.url)
    }

    fun updatePreferences(transform: (BrowserPreferencesState) -> BrowserPreferencesState) {
        viewModelScope.launch {
            repository.preferencesStore.update(transform)
        }
    }

    fun showMessage(message: String) {
        _uiState.update {
            it.copy(
                snackbarMessage = message,
                canUndoClosedTabs = false,
                canUndoDismissedMostVisited = false
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update {
            it.copy(
                snackbarMessage = null,
                canUndoClosedTabs = false,
                canUndoDismissedMostVisited = false
            )
        }
    }

    fun getWebViewForTab(tab: TabSession): WebView {
        return sessionManager.getOrCreateWebView(
            tabId = tab.id,
            isIncognito = tab.isIncognito,
            isDesktopMode = tab.isDesktopMode,
            textZoomPercent = tab.textZoomPercent
        )
    }

    fun dismissFullscreenVideo() {
        _fullscreenCustomView.value?.second?.onCustomViewHidden()
        _fullscreenCustomView.value = null
    }

    fun handleClearOnExitIfNeeded() {
        val prefs = _uiState.value.preferences
        if (prefs.clearHistoryOnExit) {
            viewModelScope.launch(Dispatchers.IO) { repository.clearAllHistory() }
        }
        if (prefs.clearCacheOnExit) {
            sessionManager.clearAllBrowserCacheAndCookies(clearCookies = false)
        }
    }

    private fun captureActiveTabThumbnail() {
        val active = _uiState.value.activeTab ?: return
        if (active.isStartPage) return
        val bmp = sessionManager.captureThumbnail(active.id) ?: return
        updateTab(active.id) { it.copy(thumbnail = bmp) }
    }

    private fun updateTab(tabId: String, transform: (TabSession) -> TabSession) {
        _uiState.update { state ->
            state.copy(tabs = state.tabs.map { if (it.id == tabId) transform(it) else it })
        }
    }

    // --- WebViewSessionListener Callbacks ---

    override fun onPageStarted(tabId: String, url: String) {
        updateTab(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                progress = 15,
                hasError = false,
                errorDescription = "",
                isReaderEligible = false
            )
        }
    }

    override fun onPageFinished(
        tabId: String,
        url: String,
        title: String,
        loadTimeMs: Long?,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId }
        updateTab(tabId) {
            it.copy(
                url = url,
                title = title,
                isLoading = false,
                progress = 100,
                loadTimeMs = loadTimeMs ?: it.loadTimeMs,
                canGoBack = canGoBack,
                canGoForward = canGoForward
            )
        }
        if (tab != null && !tab.isIncognito && url.startsWith("http")) {
            analyticsTracker.recordPageVisit(isIncognito = false)
            viewModelScope.launch { repository.recordHistory(title, url) }
        }
        if (_uiState.value.activeTabId == tabId) {
            _uiState.update { state ->
                state.copy(autofillMatchesForActiveTab = computeAutofillMatches(state))
            }
        }
    }

    override fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = progress < 100) }
    }

    override fun onTitleChanged(tabId: String, title: String) {
        updateTab(tabId) { it.copy(title = title) }
    }

    override fun onTrackerBlocked(tabId: String, url: String) {
        updateTab(tabId) { it.copy(blockedCount = it.blockedCount + 1) }
        recordBlockedTelemetry(1L)
    }

    override fun onTrackingParamsStripped(tabId: String, count: Int) {
        if (count <= 0) return
        updateTab(tabId) { it.copy(strippedParamsCount = it.strippedParamsCount + count) }
        recordBlockedTelemetry(count.toLong())
    }

    override fun onPopupOrRedirectBlocked(tabId: String, url: String) {
        updateTab(tabId) {
            it.copy(
                popupsBlockedCount = it.popupsBlockedCount + 1,
                blockedCount = it.blockedCount + 1
            )
        }
        recordBlockedTelemetry(1L)
    }

    private fun recordBlockedTelemetry(increment: Long) {
        if (increment <= 0L) return
        synchronized(this) {
            pendingBlockedIncrements += increment
        }
        if (blockedFlushJob?.isActive != true) {
            blockedFlushJob = viewModelScope.launch {
                delay(1200)
                val delta = synchronized(this@BrowserViewModel) {
                    val d = pendingBlockedIncrements
                    pendingBlockedIncrements = 0L
                    d
                }
                if (delta > 0L) {
                    repository.preferencesStore.incrementTrackersBlocked(delta)
                }
            }
        }
    }

    override fun onPageError(tabId: String, description: String) {
        updateTab(tabId) {
            it.copy(isLoading = false, progress = 0, hasError = true, errorDescription = description)
        }
    }

    override fun onFindResult(tabId: String, activeMatchOrdinal: Int, numberOfMatches: Int) {
        if (_uiState.value.activeTabId == tabId) {
            _uiState.update {
                it.copy(
                    findInPage = it.findInPage.copy(
                        activeMatchIndex = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0,
                        totalMatches = numberOfMatches
                    )
                )
            }
        }
    }

    override fun onScrollDirectionChanged(scrollingDown: Boolean) {
        val state = _uiState.value
        if (!state.preferences.autoHideToolbarOnScroll || state.findInPage.isActive) return
        val shouldShow = !scrollingDown
        if (state.isChromeVisible != shouldShow) {
            _uiState.update { it.copy(isChromeVisible = shouldShow) }
        }
    }

    override fun onContextMenuRequested(payload: ContextMenuPayload) {
        _uiState.update {
            it.copy(
                contextMenuPayload = payload,
                activeSheet = ActiveBottomSheet.CONTEXT_MENU
            )
        }
    }

    override fun onDownloadRequested(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        downloadEngine.startDownload(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            onStarted = { fileName ->
                val isApk = isApkDownload(fileName, mimeType.orEmpty())
                analyticsTracker.recordDownloadStarted(fileName, mimeType.orEmpty(), isApk)
                showMessage("Downloading $fileName")
            }
        )
    }

    override fun onFullscreenShow(view: View, callback: WebChromeClient.CustomViewCallback) {
        _fullscreenCustomView.value = view to callback
    }

    override fun onFullscreenHide() {
        _fullscreenCustomView.value = null
    }

    override fun onOpenNewTabUrl(url: String, isIncognito: Boolean) {
        createNewTab(url = url, isIncognito = isIncognito, switchToTab = true)
    }

    override fun onLoginFormDetected(tabId: String, pageUrl: String) {
        val state = _uiState.value
        if (!state.preferences.passwordManagerEnabled || state.activeTabId != tabId) return
        val matches = passwordStore.findForHost(pageUrl)
        if (matches.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    autofillMatchesForActiveTab = matches,
                    autofillBannerDismissedForHost = null
                )
            }
        }
    }

    override fun onLoginCredentialsCaptured(
        tabId: String,
        originUrl: String,
        username: String,
        password: String
    ) {
        val state = _uiState.value
        val tab = state.tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.isIncognito) return
        if (!state.preferences.passwordManagerEnabled || !state.preferences.autoPromptSavePasswords) return
        val host = SwiftShieldEngine.normalizeHost(originUrl)
        if (host.isBlank()) return
        if (passwordStore.isHostNeverSave(host)) return
        if (passwordStore.hasExactCredential(host, username, password)) return

        val isUpdate = username.isNotBlank() && passwordStore.hasUsernameForHost(host, username)
        _uiState.update {
            it.copy(
                pendingCredentialPrompt = PendingCredentialPrompt(
                    host = host,
                    originUrl = originUrl,
                    username = username,
                    password = password,
                    isUpdate = isUpdate
                ),
                activeSheet = ActiveBottomSheet.SAVE_PASSWORD,
                isChromeVisible = true
            )
        }
    }

    override fun onReaderEligibilityDetected(tabId: String, eligible: Boolean) {
        updateTab(tabId) { it.copy(isReaderEligible = eligible) }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.destroyAll()
    }

    companion object {
        private const val TAG = "BrowserViewModel"

        internal fun parseReaderJson(jsonStr: String?, url: String, fallbackTitle: String): ReaderArticle? {
            if (jsonStr.isNullOrBlank()) return null
            return runCatching {
                val obj = JSONObject(jsonStr)
                val blocksArray = obj.optJSONArray("blocks")
                val blocks = mutableListOf<ReaderBlock>()
                if (blocksArray != null) {
                    for (i in 0 until blocksArray.length()) {
                        val b = blocksArray.getJSONObject(i)
                        val text = b.optString("text", "").trim()
                        if (text.isNotEmpty()) {
                            blocks.add(
                                ReaderBlock(
                                    kind = b.optString("kind", "paragraph"),
                                    text = text
                                )
                            )
                        }
                    }
                }
                if (blocks.isEmpty()) return null
                ReaderArticle(
                    title = obj.optString("title").ifBlank { fallbackTitle },
                    byline = obj.optString("byline", ""),
                    siteName = obj.optString("siteName", ""),
                    url = url,
                    wordCount = obj.optInt("wordCount", blocks.sumOf { it.text.split(Regex("\\s+")).size }),
                    blocks = blocks,
                    publishedDate = obj.optString("publishedDate", ""),
                    leadImageUrl = obj.optString("leadImageUrl", ""),
                    strippedClutterCount = obj.optInt("strippedClutterCount", 0)
                )
            }.getOrNull()
        }
    }
}
