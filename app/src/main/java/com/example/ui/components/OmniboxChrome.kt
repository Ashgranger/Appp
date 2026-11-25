package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OmniboxPosition
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.FindInPageState
import com.example.viewmodel.OmniboxSuggestion
import com.example.viewmodel.SearchEngine
import com.example.viewmodel.SuggestionKind
import com.example.viewmodel.TabGroup
import com.example.viewmodel.TabSession

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserOmniboxBar(
    tab: TabSession,
    activeTabGroup: TabGroup? = null,
    tabCount: Int,
    position: OmniboxPosition,
    isSiteProtected: Boolean,
    visible: Boolean,
    findInPage: FindInPageState,
    onOmniboxClick: () -> Unit,
    onShieldClick: () -> Unit,
    onReaderClick: () -> Unit,
    onBackClick: () -> Unit,
    onBackLongClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadOrStopClick: () -> Unit,
    onHomeClick: () -> Unit,
    onTabSwitcherClick: () -> Unit,
    onTabSwitcherLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    onFindQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFind: () -> Unit
) {
    val isBottom = position == OmniboxPosition.BOTTOM

    AnimatedVisibility(
        visible = visible || findInPage.isActive,
        enter = expandVertically(spring()) + fadeIn(),
        exit = shrinkVertically(spring()) + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isBottom) Modifier.navigationBarsPadding().imePadding()
                    else Modifier.statusBarsPadding()
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val animatedProgress by animateFloatAsState(
                    targetValue = if (tab.isLoading && tab.progress in 1..99) tab.progress / 100f else 0f,
                    label = "load_progress"
                )

                // Hairline accent progress bar (2dp)
                if (tab.isLoading && tab.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                }

                if (findInPage.isActive) {
                    FindInPageStrip(
                        state = findInPage,
                        onQueryChange = onFindQueryChange,
                        onFindNext = onFindNext,
                        onClose = onCloseFind
                    )
                } else if (!tab.isStartPage) {
                    // Ultra-minimal Single-Row Browsing Bar (~48dp height) for maximum webpage space
                    val totalPageBlocked = tab.blockedCount + tab.strippedParamsCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Back (Long-press shows tab history)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onBackClick,
                                    onLongClick = onBackLongClick
                                )
                                .testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Go back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Compact Floating URL Capsule with inline Shield, Domain, Reader (when eligible), and Reload
                        Surface(
                            shape = RoundedCornerShape(19.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                            border = BorderStroke(
                                0.8.dp,
                                if (tab.isIncognito) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .clickable(onClick = onOmniboxClick)
                                .testTag("omnibox_pill")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (animatedProgress > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Inline SwiftShield button
                                    Surface(
                                        onClick = onShieldClick,
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSiteProtected) {
                                            ShieldEmerald.copy(alpha = 0.14f)
                                        } else {
                                            WarningAmber.copy(alpha = 0.14f)
                                        },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("shield_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSiteProtected) Icons.Filled.Shield else Icons.Outlined.Shield,
                                                contentDescription = "SwiftShield status",
                                                tint = if (isSiteProtected) ShieldEmerald else WarningAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            if (totalPageBlocked > 0) {
                                                Text(
                                                    text = totalPageBlocked.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSiteProtected) ShieldEmerald else WarningAmber
                                                )
                                            }
                                        }
                                    }

                                    // Compact HTTPS / Incognito indicator
                                    if (tab.isIncognito || !tab.isSecure) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                                            contentDescription = "Connection status",
                                            tint = if (tab.isIncognito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    Text(
                                        text = tab.displayHost,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (tab.isDesktopMode) {
                                        Icon(
                                            imageVector = Icons.Filled.DesktopWindows,
                                            contentDescription = "Desktop mode active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Show 1-tap Reader icon only when article content is detected
                                    if (!tab.isLoading && tab.isReaderEligible) {
                                        IconButton(
                                            onClick = onReaderClick,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .testTag("omnibox_reader_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Article,
                                                contentDescription = "Open Reading Mode",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onReloadOrStopClick,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .testTag("reload_stop_button")
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                                            contentDescription = if (tab.isLoading) "Stop loading" else "Reload page",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Tab Switcher Counter (long-press opens new tab)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onTabSwitcherClick,
                                    onLongClick = onTabSwitcherLongClick
                                )
                                .testTag("tab_switcher_button")
                        ) {
                            val groupAccent = activeTabGroup?.color?.color
                            val counterColor = when {
                                groupAccent != null -> groupAccent
                                tab.isIncognito -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            groupAccent?.copy(alpha = 0.14f)
                                                ?: androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .border(
                                            width = 1.8.dp,
                                            color = counterColor,
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                ) {
                                    Text(
                                        text = if (tabCount > 99) "∞" else tabCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = counterColor
                                    )
                                }
                                if (groupAccent != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(groupAccent)
                                            .border(
                                                width = 1.2.dp,
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = CircleShape
                                            )
                                            .testTag("tab_switcher_group_dot")
                                    )
                                }
                            }
                        }

                        // Browser Tools Menu
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tools_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Browser tools and settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Slim single navigation row on Start Page (48dp height, zero wasted margin)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    enabled = false,
                                    onClick = onBackClick,
                                    onLongClick = onBackLongClick
                                )
                                .testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Go back",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onForwardClick,
                            enabled = tab.canGoForward,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("nav_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Go forward",
                                tint = if (tab.canGoForward) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("nav_home_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Start Page",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onTabSwitcherClick,
                                    onLongClick = onTabSwitcherLongClick
                                )
                                .testTag("tab_switcher_button")
                        ) {
                            val groupAccent = activeTabGroup?.color?.color
                            val counterColor = when {
                                groupAccent != null -> groupAccent
                                tab.isIncognito -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            groupAccent?.copy(alpha = 0.14f)
                                                ?: androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .border(
                                            width = 1.8.dp,
                                            color = counterColor,
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                ) {
                                    Text(
                                        text = if (tabCount > 99) "∞" else tabCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = counterColor
                                    )
                                }
                                if (groupAccent != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(groupAccent)
                                            .border(
                                                width = 1.2.dp,
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = CircleShape
                                            )
                                            .testTag("tab_switcher_group_dot")
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("tools_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Browser tools and settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FindInPageStrip(
    state: FindInPageState,
    onQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        BasicTextField(
            value = state.query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onFindNext(true) }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag("find_in_page_input"),
            decorationBox = { inner ->
                if (state.query.isEmpty()) {
                    Text(
                        text = "Find in page...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )
        Text(
            text = if (state.query.isBlank()) "" else "${state.activeMatchIndex}/${state.totalMatches}",
            style = MonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            onClick = { onFindNext(false) },
            enabled = state.totalMatches > 0,
            modifier = Modifier.size(40.dp).testTag("find_prev_button")
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
        }
        IconButton(
            onClick = { onFindNext(true) },
            enabled = state.totalMatches > 0,
            modifier = Modifier.size(40.dp).testTag("find_next_button")
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp).testTag("find_close_button")
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close find in page")
        }
    }
}

@Composable
fun OmniboxSearchOverlay(
    query: String,
    suggestions: List<OmniboxSuggestion>,
    isLoadingRemoteSuggestions: Boolean,
    liveSuggestionsEnabled: Boolean,
    instantAnswer: String?,
    matchingOpenTabs: List<TabSession>,
    searchEngines: List<SearchEngine>,
    activeSearchEngine: SearchEngine,
    isBottomBar: Boolean,
    isIncognito: Boolean,
    onQueryChange: (String, String) -> Unit,
    onSubmit: (String, String?) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSwitchToOpenTab: (String) -> Unit,
    onCopyInstantAnswer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    val clipboardText = remember { clipboard.getText()?.text?.trim().orEmpty() }
    var selectedEngineId by remember(activeSearchEngine.id) { mutableStateOf(activeSearchEngine.id) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(0, query.length)))
    }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = TextFieldValue(query, TextRange(query.length))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val inputCard: @Composable () -> Unit = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    textFieldValue = it
                                    onQueryChange(it.text, selectedEngineId)
                                },
                                singleLine = true,
                                textStyle = MonoCodeStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    onSubmit(textFieldValue.text, selectedEngineId)
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("omnibox_input"),
                                decorationBox = { inner ->
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Search, type URL, or calculate (e.g. 24*15)...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        textFieldValue = TextFieldValue("")
                                        onQueryChange("", selectedEngineId)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear URL", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp).testTag("omnibox_dismiss_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Cancel search")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Instant Search Engine Switcher Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchEngines.forEach { engine ->
                                FilterChip(
                                    selected = selectedEngineId == engine.id,
                                    onClick = {
                                        selectedEngineId = engine.id
                                        onQueryChange(textFieldValue.text, engine.id)
                                    },
                                    label = { Text(engine.name, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        // Quick URL Suffix Helper Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("https://", ".com", ".org", ".dev", ".io", "/").forEach { token ->
                                AssistChip(
                                    onClick = {
                                        val next = textFieldValue.text + token
                                        textFieldValue = TextFieldValue(next, TextRange(next.length))
                                        onQueryChange(next, selectedEngineId)
                                    },
                                    label = { Text(token, style = MonoCodeStyle) }
                                )
                            }
                        }

                        if (isLoadingRemoteSuggestions) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .testTag("omnibox_suggestions_loading"),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else if (isIncognito && liveSuggestionsEnabled && textFieldValue.text.trim().length >= 2) {
                            Row(
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Live search suggestions paused in Incognito",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!isBottomBar) {
                inputCard()
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = isBottomBar,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Chrome / Arc-inspired Instant Math Calculator Card
                if (instantAnswer != null) {
                    item(key = "instant_calculator_answer") {
                        Surface(
                            onClick = {
                                clipboard.setText(AnnotatedString(instantAnswer))
                                onCopyInstantAnswer(instantAnswer)
                            },
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("omnibox_instant_answer_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Calculate,
                                    contentDescription = null,
                                    tint = ShieldEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Instant Calculation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "= $instantAnswer",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy result",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Chrome / Firefox-inspired "Switch to Open Tab" Cards
                if (matchingOpenTabs.isNotEmpty()) {
                    items(matchingOpenTabs, key = { "open_tab_${it.id}" }) { openTab ->
                        Surface(
                            onClick = { onSwitchToOpenTab(openTab.id) },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .testTag("omnibox_switch_tab_${openTab.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tab,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = openTab.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = openTab.displayHost,
                                        style = MonoCodeStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "Switch Tab",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Clipboard Quick Paste Chip
                if (clipboardText.isNotEmpty() && clipboardText != query) {
                    item(key = "clipboard_paste") {
                        Surface(
                            onClick = { onSubmit(clipboardText, selectedEngineId) },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("clipboard_go_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Link from clipboard",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = clipboardText,
                                        style = MonoCodeStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Direct Search Action Row
                if (textFieldValue.text.isNotBlank()) {
                    item(key = "direct_go") {
                        val engineName = searchEngines.firstOrNull { it.id == selectedEngineId }?.name ?: "Web"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubmit(textFieldValue.text, selectedEngineId) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("omnibox_submit_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Go or search with $engineName: \"${textFieldValue.text}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                items(suggestions, key = { "${it.kind}_${it.url.ifBlank { it.title }}" }) { item ->
                    val isWebSearch = item.kind == SuggestionKind.WEB_SEARCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isWebSearch) {
                                    onSubmit(item.title, selectedEngineId)
                                } else {
                                    onSuggestionClick(item.url)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(if (isWebSearch) "omnibox_web_suggestion" else "omnibox_local_suggestion"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (item.kind) {
                                SuggestionKind.BOOKMARK -> Icons.Filled.Bookmark
                                SuggestionKind.HISTORY -> Icons.Filled.History
                                SuggestionKind.WEB_SEARCH -> Icons.Filled.Search
                            },
                            contentDescription = null,
                            tint = when (item.kind) {
                                SuggestionKind.BOOKMARK -> MaterialTheme.colorScheme.primary
                                SuggestionKind.WEB_SEARCH -> MaterialTheme.colorScheme.secondary
                                SuggestionKind.HISTORY -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isWebSearch) FontWeight.Medium else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isWebSearch) {
                                Text(
                                    text = if (item.engineName.isNotBlank()) "Search with ${item.engineName}" else "Web search suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = item.url,
                                    style = MonoCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val insertText = if (isWebSearch) "${item.title} " else item.url
                                textFieldValue = TextFieldValue(insertText, TextRange(insertText.length))
                                onQueryChange(insertText, selectedEngineId)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NorthWest,
                                contentDescription = if (isWebSearch) "Refine search query" else "Insert URL into search box",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isBottomBar) {
                inputCard()
            }
        }
    }
}
