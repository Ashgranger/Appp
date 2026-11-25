package com.example.ui

import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ImageLoadingPolicy
import com.example.data.OmniboxPosition
import com.example.ui.components.BrowserOmniboxBar
import com.example.ui.components.OmniboxSearchOverlay
import com.example.ui.screens.LibraryAndDownloadsScreen
import com.example.ui.screens.PasswordVaultScreen
import com.example.ui.screens.ReaderViewOverlay
import com.example.ui.screens.SettingsAndFiltersScreen
import com.example.ui.screens.StartPageView
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.screens.ViewSourceOverlay
import com.example.ui.sheets.AutofillFloatingBar
import com.example.ui.sheets.BrowserToolsSheet
import com.example.ui.sheets.FeedbackBottomSheet
import com.example.ui.sheets.LinkContextMenuSheet
import com.example.ui.sheets.SavePasswordPromptSheet
import com.example.ui.sheets.SwiftShieldPanelSheet
import com.example.ui.sheets.TabHistoryBottomSheet
import com.example.viewmodel.ActiveBottomSheet
import com.example.viewmodel.ActiveOverlay
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.SettingsSection
import com.example.viewmodel.TabSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserRootScreen(viewModel: BrowserViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullscreenPair by viewModel.fullscreenCustomView.collectAsStateWithLifecycle()
    val activeTab = state.activeTab ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar with optional Undo closed tabs or Undo dismissed Most Visited action
    LaunchedEffect(state.snackbarMessage, state.canUndoClosedTabs, state.canUndoDismissedMostVisited) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        val hasUndo = state.canUndoClosedTabs || state.canUndoDismissedMostVisited
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = if (hasUndo) "Undo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            when {
                state.canUndoClosedTabs -> viewModel.undoCloseTabs()
                state.canUndoDismissedMostVisited -> viewModel.undoDismissMostVisited()
                else -> viewModel.clearSnackbar()
            }
        } else {
            viewModel.clearSnackbar()
        }
    }

    // Predictive Back Handler hierarchy: Fullscreen -> Sheets -> Overlays -> Find -> WebView Back
    val shouldInterceptBack = fullscreenPair != null ||
        state.activeSheet != ActiveBottomSheet.NONE ||
        state.activeOverlay != ActiveOverlay.NONE ||
        state.findInPage.isActive ||
        !activeTab.isStartPage

    BackHandler(enabled = shouldInterceptBack) {
        when {
            fullscreenPair != null -> viewModel.dismissFullscreenVideo()
            state.activeSheet != ActiveBottomSheet.NONE -> viewModel.setBottomSheet(ActiveBottomSheet.NONE)
            state.activeOverlay != ActiveOverlay.NONE -> viewModel.setOverlay(ActiveOverlay.NONE)
            state.findInPage.isActive -> viewModel.closeFindInPage()
            else -> viewModel.goBack()
        }
    }

    val isBottomBar = state.preferences.omniboxPosition == OmniboxPosition.BOTTOM
    val isSiteProtected = viewModel.shieldEngine.isSiteProtected(
        if (activeTab.isStartPage) null else activeTab.displayHost
    )
    val visibleTabCount = if (activeTab.isIncognito) state.incognitoTabs.size else state.normalTabs.size

    fun shareUrl(url: String, title: String = "") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
        }
        runCatching {
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
        }
    }

    val omniboxBarContent: @Composable () -> Unit = {
        BrowserOmniboxBar(
            tab = activeTab,
            activeTabGroup = state.activeTabGroup,
            tabCount = visibleTabCount,
            position = state.preferences.omniboxPosition,
            isSiteProtected = isSiteProtected,
            visible = state.isChromeVisible,
            findInPage = state.findInPage,
            onOmniboxClick = { viewModel.openOmnibox() },
            onShieldClick = { viewModel.setBottomSheet(ActiveBottomSheet.SHIELD_PANEL) },
            onReaderClick = { viewModel.openReaderView() },
            onBackClick = { viewModel.goBack() },
            onBackLongClick = { viewModel.openTabHistorySheet() },
            onForwardClick = { viewModel.goForward() },
            onReloadOrStopClick = { viewModel.reloadOrStop() },
            onHomeClick = { viewModel.goHome() },
            onTabSwitcherClick = { viewModel.openTabSwitcher() },
            onTabSwitcherLongClick = { viewModel.createNewTab(isIncognito = activeTab.isIncognito) },
            onMenuClick = { viewModel.setBottomSheet(ActiveBottomSheet.TOOLS_MENU) },
            onFindQueryChange = { viewModel.updateFindInPageQuery(it) },
            onFindNext = { viewModel.findNextMatch(it) },
            onCloseFind = { viewModel.closeFindInPage() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            topBar = {
                if (!isBottomBar) omniboxBarContent()
            },
            bottomBar = {
                if (isBottomBar) omniboxBarContent()
            }
        ) { innerPadding ->
            val topSafeInset = isBottomBar || (!state.isChromeVisible && !state.findInPage.isActive)
            val bottomSafeInset = !isBottomBar && activeTab.isStartPage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (topSafeInset) Modifier.statusBarsPadding() else Modifier)
                    .then(if (bottomSafeInset) Modifier.navigationBarsPadding() else Modifier)
            ) {
                when {
                    activeTab.isStartPage -> {
                        StartPageView(
                            isIncognito = activeTab.isIncognito,
                            preferences = state.preferences,
                            activeSearchEngineName = state.activeSearchEngine.name,
                            mostVisitedSites = state.mostVisitedSites,
                            speedDials = state.speedDials,
                            selectedNewsCategory = state.selectedNewsCategory,
                            newsArticles = state.newsArticles,
                            isLoadingNews = state.isLoadingNews,
                            newsError = state.newsError,
                            onOpenOmnibox = { viewModel.openOmnibox() },
                            onNavigateUrl = { viewModel.navigateActiveTab(it) },
                            onOpenInNewTab = { url, incognito ->
                                viewModel.createNewTab(url = url, isIncognito = incognito)
                            },
                            onSwitchMode = { incognito -> viewModel.switchBrowsingMode(incognito) },
                            onBurnSession = { viewModel.burnSessionData() },
                            onPinMostVisited = { viewModel.pinMostVisitedSite(it) },
                            onDismissMostVisited = { viewModel.dismissMostVisitedHost(it) },
                            onRestoreDismissedMostVisited = { viewModel.restoreAllDismissedMostVisited() },
                            onAddSpeedDial = { title, url ->
                                viewModel.saveCustomBookmark(title, url, "Speed Dial", isSpeedDial = true)
                            },
                            onDeleteSpeedDial = { viewModel.deleteBookmark(it) },
                            onSelectNewsCategory = { viewModel.selectNewsCategory(it) },
                            onRefreshNews = { viewModel.refreshNewsFeed(forceRefresh = true) },
                            onOpenSettings = { viewModel.openSettings(SettingsSection.APPEARANCE) }
                        )
                    }
                    activeTab.hasError -> {
                        WebErrorCard(
                            url = activeTab.url,
                            description = activeTab.errorDescription,
                            onRetry = { viewModel.reloadOrStop() },
                            onGoHome = { viewModel.goHome() }
                        )
                    }
                    else -> {
                        val activeWebView = viewModel.getWebViewForTab(activeTab)
                        PullToRefreshBox(
                            isRefreshing = activeTab.isLoading && activeTab.progress < 75,
                            onRefresh = { viewModel.reloadOrStop() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { container ->
                                    if (container.childCount != 1 || container.getChildAt(0) !== activeWebView) {
                                        container.removeAllViews()
                                        (activeWebView.parent as? ViewGroup)?.removeView(activeWebView)
                                        container.addView(
                                            activeWebView,
                                            FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("webview_host")
                            )
                        }

                        // Contextual Local Encrypted Password Auto-Fill Floating Bar
                        AutofillFloatingBar(
                            visible = state.preferences.passwordManagerEnabled &&
                                state.autofillMatchesForActiveTab.isNotEmpty() &&
                                state.autofillBannerDismissedForHost != activeTab.displayHost,
                            matches = state.autofillMatchesForActiveTab,
                            onAutofill = { cred -> viewModel.autofillActiveTab(cred) },
                            onDismiss = { viewModel.dismissAutofillBanner() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // Full-Screen Overlays
        when (state.activeOverlay) {
            ActiveOverlay.OMNIBOX_SEARCH -> {
                OmniboxSearchOverlay(
                    query = state.omniboxQuery,
                    suggestions = state.omniboxSuggestions,
                    isLoadingRemoteSuggestions = state.isLoadingRemoteSuggestions,
                    liveSuggestionsEnabled = state.preferences.searchSuggestionsEnabled,
                    instantAnswer = state.omniboxInstantAnswer,
                    matchingOpenTabs = state.matchingOpenTabs,
                    searchEngines = state.searchEngines,
                    activeSearchEngine = state.activeSearchEngine,
                    isBottomBar = isBottomBar,
                    isIncognito = activeTab.isIncognito,
                    onQueryChange = { text, engineId -> viewModel.onOmniboxQueryChange(text, engineId) },
                    onSubmit = { text, engineId -> viewModel.submitOmnibox(text, engineId) },
                    onSuggestionClick = { viewModel.navigateActiveTab(it) },
                    onSwitchToOpenTab = { viewModel.selectTab(it) },
                    onCopyInstantAnswer = { ans -> viewModel.showMessage("Copied $ans") },
                    onDismiss = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.TAB_SWITCHER -> {
                TabSwitcherScreen(
                    tabs = state.tabs,
                    tabGroups = state.tabGroups,
                    activeTabId = state.activeTabId,
                    isIncognitoSegment = state.tabSwitcherIncognitoSegment,
                    activeGroupFilterId = state.activeTabGroupFilterId,
                    layoutMode = state.effectiveTabLayout,
                    isMultiSelectMode = state.isTabMultiSelectMode,
                    selectedTabIds = state.selectedTabIds,
                    onSegmentChange = { viewModel.setTabSwitcherSegment(it) },
                    onFilterGroupChange = { viewModel.setTabGroupFilter(it) },
                    onToggleLayoutMode = { viewModel.toggleTabLayoutMode() },
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onCreateNewTab = { incognito, groupId ->
                        viewModel.createNewTab(isIncognito = incognito, groupId = groupId)
                    },
                    onReorderTab = { fromId, toId -> viewModel.reorderTab(fromId, toId) },
                    onToggleMultiSelect = { viewModel.setTabMultiSelectMode(it) },
                    onToggleTabSelected = { viewModel.toggleTabSelected(it) },
                    onSelectAll = { viewModel.selectAllTabsInCurrentSegment() },
                    onCloseSelected = { viewModel.closeSelectedTabs() },
                    onCloseOthers = { viewModel.closeOtherTabsInSegment() },
                    onCloseAll = { viewModel.closeAllTabsInSegment() },
                    onCreateGroup = { name, color, ids, incognito ->
                        viewModel.createTabGroup(name, color, ids, incognito)
                    },
                    onAssignTabToGroup = { tabId, groupId -> viewModel.assignTabToGroup(tabId, groupId) },
                    onRenameRecolorGroup = { id, name, color -> viewModel.renameAndRecolorGroup(id, name, color) },
                    onToggleGroupCollapsed = { viewModel.toggleGroupCollapsed(it) },
                    onUngroupCategory = { viewModel.ungroupCategory(it) },
                    onCloseGroup = { viewModel.closeTabGroup(it) },
                    onDismiss = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.READER_VIEW -> {
                ReaderViewOverlay(
                    article = state.readerArticle,
                    isLoading = state.isExtractingReader,
                    fontSizeSp = state.preferences.readerFontSizeSp,
                    paperTheme = state.preferences.readerPaperTheme,
                    fontChoice = state.preferences.readerFontChoice,
                    lineSpacing = state.preferences.readerLineSpacing,
                    onFontSizeChange = { size -> viewModel.updatePreferences { it.copy(readerFontSizeSp = size) } },
                    onPaperThemeChange = { theme -> viewModel.updatePreferences { it.copy(readerPaperTheme = theme) } },
                    onFontChoiceChange = { choice -> viewModel.updatePreferences { it.copy(readerFontChoice = choice) } },
                    onLineSpacingChange = { spacing -> viewModel.updatePreferences { it.copy(readerLineSpacing = spacing) } },
                    onShareArticle = { title, url -> shareUrl(url, title) },
                    onCopiedArticle = { viewModel.showMessage("Copied clean article text") },
                    onClose = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.VIEW_SOURCE -> {
                ViewSourceOverlay(
                    url = activeTab.url,
                    sourceCode = state.pageSourceCode,
                    isLoading = state.isExtractingSource,
                    onClose = { viewModel.setOverlay(ActiveOverlay.NONE) },
                    onCopied = { viewModel.showMessage("Copied HTML source") }
                )
            }
            ActiveOverlay.LIBRARY -> {
                LibraryAndDownloadsScreen(
                    activeTab = state.activeLibraryTab,
                    bookmarks = state.bookmarks,
                    history = state.history,
                    downloads = state.downloads,
                    downloadSpeedsById = state.downloadSpeedsById,
                    onTabChange = { viewModel.setLibraryTab(it) },
                    onOpenUrl = { viewModel.navigateActiveTab(it) },
                    onAddBookmark = { title, url, folder, dial ->
                        viewModel.saveCustomBookmark(title, url, folder, dial)
                    },
                    onToggleSpeedDial = { viewModel.toggleSpeedDialPin(it) },
                    onDeleteBookmark = { viewModel.deleteBookmark(it) },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onClearBrowsingData = { h, c, ck, since -> viewModel.clearBrowsingData(h, c, ck, since) },
                    onStartManualDownload = { url -> viewModel.startManualDownload(url) },
                    onPauseDownload = { viewModel.pauseDownload(it) },
                    onResumeDownload = { viewModel.resumeDownload(it) },
                    onCancelDownload = { viewModel.cancelDownload(it) },
                    onDeleteDownload = { viewModel.deleteDownloadRecord(it) },
                    onClearDownloads = { viewModel.clearDownloadRecords() },
                    onOpenDownloadedFile = { viewModel.openDownloadedFile(context, it) },
                    onShareDownloadedFile = { viewModel.shareDownloadedFile(context, it) },
                    onClose = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.PASSWORDS -> {
                PasswordVaultScreen(
                    credentials = state.savedCredentials,
                    neverSaveHosts = state.neverSavePasswordHosts,
                    onSaveCredential = { host, user, pass, existingId ->
                        viewModel.saveManualCredential(host, user, pass, existingId)
                    },
                    onDeleteCredential = { viewModel.deleteCredential(it) },
                    onRemoveNeverSaveHost = { viewModel.removeNeverSavePasswordHost(it) },
                    onClearAll = { viewModel.clearAllSavedCredentials() },
                    onOpenUrl = { viewModel.navigateActiveTab(it) },
                    onCopiedMessage = { viewModel.showMessage(it) },
                    onClose = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.SETTINGS -> {
                SettingsAndFiltersScreen(
                    section = state.activeSettingsSection,
                    preferences = state.preferences,
                    searchEngines = state.searchEngines,
                    filterSubscriptions = state.filterSubscriptions,
                    userCustomRules = state.userCustomFilterRules,
                    allowlistedHosts = state.allowlistedHosts,
                    isUpdatingFilters = state.isUpdatingFilterLists,
                    activeRuleCount = state.activeRuleCount,
                    savedPasswordCount = state.savedCredentials.size,
                    onSectionChange = { viewModel.setSettingsSection(it) },
                    onUpdatePreferences = { viewModel.updatePreferences(it) },
                    onSelectSearchEngine = { viewModel.selectDefaultSearchEngine(it) },
                    onAddCustomSearchEngine = { name, tpl -> viewModel.addCustomSearchEngine(name, tpl) },
                    onDeleteCustomSearchEngine = { viewModel.deleteCustomSearchEngine(it) },
                    onToggleFilterSubscription = { id, enabled -> viewModel.toggleFilterSubscription(id, enabled) },
                    onAddCustomFilterSubscription = { name, url -> viewModel.addCustomFilterSubscription(name, url) },
                    onDeleteCustomFilterSubscription = { viewModel.deleteCustomFilterSubscription(it) },
                    onUpdateFilterListsNow = { viewModel.updateFilterListsNow(silent = false) },
                    onAddCustomBlockedDomain = { viewModel.addCustomBlockedDomain(it) },
                    onRemoveCustomBlockedDomain = { viewModel.removeCustomBlockedDomain(it) },
                    onRemoveAllowlistedHost = { viewModel.removeAllowlistedHost(it) },
                    onRestoreDismissedMostVisited = { viewModel.restoreAllDismissedMostVisited() },
                    onOpenPasswordVault = { viewModel.openPasswordVault() },
                    onOpenFeedback = { viewModel.openFeedbackSheet() },
                    onClearAllBrowsingData = { viewModel.clearBrowsingData(true, true, true, 0L) },
                    onClose = { viewModel.setOverlay(ActiveOverlay.NONE) }
                )
            }
            ActiveOverlay.NONE -> Unit
        }

        // Bottom Sheets
        when (state.activeSheet) {
            ActiveBottomSheet.TOOLS_MENU -> {
                val isBookmarked = state.bookmarks.any { it.url == activeTab.url }
                BrowserToolsSheet(
                    tab = activeTab,
                    activeTabGroup = state.activeTabGroup,
                    tabGroups = state.tabGroups.filter { it.isIncognito == activeTab.isIncognito },
                    isBookmarked = isBookmarked,
                    savedPasswordCount = state.savedCredentials.size,
                    preferences = state.preferences,
                    onDismiss = { viewModel.setBottomSheet(ActiveBottomSheet.NONE) },
                    onGoHome = { viewModel.goHome() },
                    onGoForward = { viewModel.goForward() },
                    onNewTab = { viewModel.createNewTab(isIncognito = false) },
                    onNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
                    onToggleBookmark = { viewModel.toggleBookmarkActiveTab(pinToSpeedDial = false) },
                    onPinToSpeedDial = { viewModel.toggleBookmarkActiveTab(pinToSpeedDial = true) },
                    onToggleDesktopMode = { viewModel.toggleDesktopModeForActiveTab() },
                    onOpenReaderView = { viewModel.openReaderView() },
                    onOpenFindInPage = { viewModel.openFindInPage() },
                    onToggleForceDark = {
                        viewModel.updatePreferences { it.copy(forceDarkWebPages = !it.forceDarkWebPages) }
                    },
                    onZoomChange = { viewModel.setZoomForActiveTab(it) },
                    onTranslatePage = { viewModel.translateActivePage() },
                    onAssignTabToGroup = { groupId -> viewModel.assignTabToGroup(activeTab.id, groupId) },
                    onCreateGroupForActiveTab = { name, color ->
                        viewModel.createTabGroup(name, color, setOf(activeTab.id), activeTab.isIncognito)
                    },
                    onBurnSession = { viewModel.burnSessionData() },
                    onShare = { shareUrl(activeTab.url, activeTab.title) },
                    onCopyUrl = {
                        clipboard.setText(AnnotatedString(activeTab.url))
                        viewModel.showMessage("Link copied to clipboard")
                    },
                    onPrintPage = { viewModel.printActivePage(context) },
                    onViewSource = { viewModel.openViewSource() },
                    onOpenLibrary = { viewModel.openLibrary(it) },
                    onOpenPasswords = { viewModel.openPasswordVault() },
                    onOpenFeedback = { viewModel.openFeedbackSheet() },
                    onOpenSettings = { viewModel.openSettings(it) }
                )
            }
            ActiveBottomSheet.SHIELD_PANEL -> {
                SwiftShieldPanelSheet(
                    tab = activeTab,
                    preferences = state.preferences,
                    isSiteProtected = isSiteProtected,
                    activeRuleCount = state.activeRuleCount,
                    onDismiss = { viewModel.setBottomSheet(ActiveBottomSheet.NONE) },
                    onToggleSiteProtection = { viewModel.toggleProtectionForCurrentSite() },
                    onToggleCosmetic = { enabled ->
                        viewModel.updatePreferences { it.copy(cosmeticBlockingEnabled = enabled) }
                    },
                    onToggleStripParams = { enabled ->
                        viewModel.updatePreferences { it.copy(stripTrackingParamsEnabled = enabled) }
                    },
                    onTogglePopups = { enabled ->
                        viewModel.updatePreferences { it.copy(blockPopupsAndRedirects = enabled) }
                    },
                    onToggleAnnoyances = { enabled ->
                        viewModel.updatePreferences { it.copy(blockAnnoyances = enabled) }
                    },
                    onToggleTurboMode = { enabled ->
                        viewModel.updatePreferences { it.copy(turboModeEnabled = enabled) }
                    },
                    onCycleImagePolicy = {
                        val policies = ImageLoadingPolicy.entries
                        val next = policies[(state.preferences.imageLoadingPolicy.ordinal + 1) % policies.size]
                        viewModel.updatePreferences { it.copy(imageLoadingPolicy = next) }
                    },
                    onClearSiteData = { viewModel.clearDataForCurrentSite() },
                    onOpenFilterSettings = { viewModel.openSettings(SettingsSection.SHIELD) }
                )
            }
            ActiveBottomSheet.CONTEXT_MENU -> {
                state.contextMenuPayload?.let { payload ->
                    LinkContextMenuSheet(
                        payload = payload,
                        onDismiss = { viewModel.setBottomSheet(ActiveBottomSheet.NONE) },
                        onOpenNewTab = { url -> viewModel.createNewTab(url = url, isIncognito = false) },
                        onOpenIncognito = { url -> viewModel.createNewTab(url = url, isIncognito = true) },
                        onDownloadUrl = { url, isImage ->
                            viewModel.startManualDownload(url, if (isImage) "image/*" else null)
                        },
                        onCopyUrl = { url ->
                            clipboard.setText(AnnotatedString(url))
                            viewModel.showMessage("Address copied")
                        },
                        onShareUrl = { url -> shareUrl(url) }
                    )
                }
            }
            ActiveBottomSheet.TAB_HISTORY -> {
                TabHistoryBottomSheet(
                    entries = state.tabHistoryEntries,
                    onDismiss = { viewModel.setBottomSheet(ActiveBottomSheet.NONE) },
                    onSelectIndex = { viewModel.navigateToTabHistoryIndex(it) }
                )
            }
            ActiveBottomSheet.SAVE_PASSWORD -> {
                state.pendingCredentialPrompt?.let { prompt ->
                    SavePasswordPromptSheet(
                        prompt = prompt,
                        onSave = { user, pass -> viewModel.savePendingCredential(user, pass) },
                        onNeverForSite = { viewModel.neverSaveForPendingHost() },
                        onDismiss = { viewModel.dismissSavePasswordPrompt() }
                    )
                }
            }
            ActiveBottomSheet.FEEDBACK -> {
                FeedbackBottomSheet(
                    previousFeedbackCount = state.feedbackEntries.size,
                    onSubmit = { rating, cat, msg -> viewModel.submitFeedback(rating, cat, msg) },
                    onDismiss = { viewModel.setBottomSheet(ActiveBottomSheet.NONE) }
                )
            }
            ActiveBottomSheet.NONE -> Unit
        }

        // Fullscreen Video Overlay
        fullscreenPair?.let { (customView, _) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            (customView.parent as? ViewGroup)?.removeView(customView)
                            addView(
                                customView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun WebErrorCard(
    url: String,
    description: String,
    onRetry: () -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text("Page Could Not Load", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onGoHome) {
                        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Start Page")
                    }
                    Button(onClick = onRetry, modifier = Modifier.testTag("error_retry_button")) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
