package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.BrowserPreferencesState
import com.example.data.ImageLoadingPolicy
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ContextMenuPayload
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.SettingsSection
import com.example.viewmodel.TabGroup
import com.example.viewmodel.TabGroupColor
import com.example.viewmodel.TabHistoryEntry
import com.example.viewmodel.TabSession
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserToolsSheet(
    tab: TabSession,
    activeTabGroup: TabGroup? = null,
    tabGroups: List<TabGroup> = emptyList(),
    isBookmarked: Boolean,
    savedPasswordCount: Int,
    preferences: BrowserPreferencesState,
    onDismiss: () -> Unit,
    onGoHome: () -> Unit = {},
    onGoForward: () -> Unit = {},
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPinToSpeedDial: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenReaderView: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onToggleForceDark: () -> Unit,
    onZoomChange: (Int) -> Unit,
    onTranslatePage: () -> Unit,
    onAssignTabToGroup: (String?) -> Unit = {},
    onCreateGroupForActiveTab: (String, TabGroupColor) -> Unit = { _, _ -> },
    onBurnSession: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onPrintPage: () -> Unit,
    onViewSource: () -> Unit,
    onOpenLibrary: (LibraryTab) -> Unit,
    onOpenPasswords: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenSettings: (SettingsSection) -> Unit
) {
    var showBurnConfirm by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Page Summary Banner or compact Fevro Browser identity header on Start Page
            if (tab.isStartPage) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fevro_browser_icon),
                            contentDescription = "Fevro Browser icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fevro Browser",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (tab.isIncognito) "Private Session" else "Fast • Private • Minimal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = tab.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title.ifBlank { tab.displayHost },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (tab.isSecure) Icons.Filled.Lock else Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = if (tab.isSecure) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = buildString {
                                        append(tab.displayHost)
                                        tab.loadTimeMs?.let { append(" • ${it}ms") }
                                    },
                                    style = MonoCodeStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (tab.canGoForward) {
                            IconButton(
                                onClick = { onDismiss(); onGoForward() },
                                modifier = Modifier.size(38.dp).testTag("menu_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Go forward",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDismiss(); onGoHome() },
                            modifier = Modifier.size(38.dp).testTag("menu_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Go to Start Page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolTile(
                    icon = Icons.Filled.Add,
                    label = "New Tab",
                    modifier = Modifier.weight(1f).testTag("menu_new_tab"),
                    onClick = { onDismiss(); onNewTab() }
                )
                ToolTile(
                    icon = Icons.Filled.VisibilityOff,
                    label = "Incognito",
                    modifier = Modifier.weight(1f).testTag("menu_incognito"),
                    onClick = { onDismiss(); onNewIncognitoTab() }
                )
                ToolTile(
                    icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = if (isBookmarked) "Saved" else "Bookmark",
                    highlighted = isBookmarked,
                    enabled = !tab.isStartPage,
                    modifier = Modifier.weight(1f).testTag("menu_bookmark"),
                    onClick = { onToggleBookmark(); onDismiss() }
                )
                ToolTile(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    enabled = !tab.isStartPage,
                    modifier = Modifier.weight(1f).testTag("menu_share"),
                    onClick = { onDismiss(); onShare() }
                )
            }

            // Power Browsing Toggles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolTile(
                    icon = Icons.Filled.DesktopWindows,
                    label = "Desktop",
                    highlighted = tab.isDesktopMode,
                    enabled = !tab.isStartPage,
                    modifier = Modifier.weight(1f).testTag("menu_desktop_mode"),
                    onClick = { onToggleDesktopMode(); onDismiss() }
                )
                ToolTile(
                    icon = Icons.Filled.Article,
                    label = "Reading",
                    highlighted = !tab.isStartPage && tab.isReaderEligible,
                    enabled = !tab.isStartPage,
                    modifier = Modifier.weight(1f).testTag("menu_reader_view"),
                    onClick = onOpenReaderView
                )
                ToolTile(
                    icon = Icons.Filled.FindInPage,
                    label = "Find",
                    enabled = !tab.isStartPage,
                    modifier = Modifier.weight(1f).testTag("menu_find_in_page"),
                    onClick = onOpenFindInPage
                )
                ToolTile(
                    icon = Icons.Filled.DarkMode,
                    label = "Dark Web",
                    highlighted = preferences.forceDarkWebPages,
                    modifier = Modifier.weight(1f).testTag("menu_force_dark"),
                    onClick = onToggleForceDark
                )
            }

            // Safari AA-style Page Text Zoom Stepper + Slider (50% - 200%)
            if (!tab.isStartPage) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onZoomChange((tab.textZoomPercent - 10).coerceAtLeast(50)) },
                            enabled = tab.textZoomPercent > 50,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
                        }
                        Slider(
                            value = tab.textZoomPercent.toFloat(),
                            onValueChange = { onZoomChange((it / 10f).roundToInt() * 10) },
                            valueRange = 50f..200f,
                            steps = 14,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onZoomChange((tab.textZoomPercent + 10).coerceAtMost(200)) },
                            enabled = tab.textZoomPercent < 200,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${tab.textZoomPercent}%",
                            style = MonoCodeStyle,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Library & Utilities List
            SheetMenuRow(Icons.Filled.Bookmark, "Bookmarks", onClick = { onOpenLibrary(LibraryTab.BOOKMARKS) })
            SheetMenuRow(Icons.Filled.History, "History", onClick = { onOpenLibrary(LibraryTab.HISTORY) })
            SheetMenuRow(Icons.Filled.Download, "Downloads", onClick = { onOpenLibrary(LibraryTab.DOWNLOADS) })
            SheetMenuRow(
                icon = Icons.Filled.Key,
                label = "Password Vault",
                subtitle = if (savedPasswordCount > 0) "$savedPasswordCount encrypted local login(s)" else "Local AES-256 encrypted passwords",
                tag = "menu_password_vault",
                onClick = onOpenPasswords
            )
            SheetMenuRow(
                icon = Icons.Filled.Folder,
                label = "Tab Category",
                subtitle = activeTabGroup?.let { "In category: ${it.name}" } ?: "Organize this tab into a named category",
                tag = "menu_tab_category",
                onClick = { showCategoryPicker = true }
            )

            if (!tab.isStartPage) {
                SheetMenuRow(
                    icon = Icons.Filled.Translate,
                    label = "Translate Page",
                    subtitle = "Translate this site to English",
                    tag = "menu_translate_page",
                    onClick = { onTranslatePage(); onDismiss() }
                )
                SheetMenuRow(Icons.Filled.PushPin, "Pin to Speed Dial", onClick = { onPinToSpeedDial(); onDismiss() })
                SheetMenuRow(Icons.Filled.ContentCopy, "Copy Link Address", onClick = { onCopyUrl(); onDismiss() })
                SheetMenuRow(Icons.Filled.Print, "Print / Save as PDF", onClick = onPrintPage)
                SheetMenuRow(Icons.Filled.Code, "View Page Source", onClick = onViewSource)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SheetMenuRow(
                icon = Icons.Filled.Settings,
                label = "Customization & Settings",
                subtitle = "Theme, Omnibox, Speed Engine, Filter Lists",
                onClick = { onOpenSettings(SettingsSection.APPEARANCE) },
                tag = "menu_settings"
            )
            SheetMenuRow(
                icon = Icons.Filled.Feedback,
                label = "Send Feedback",
                subtitle = "Rate the browser or share a suggestion",
                onClick = onOpenFeedback,
                tag = "menu_feedback"
            )

            // DuckDuckGo-inspired Burn All Tabs & Browsing Data Button
            OutlinedButton(
                onClick = { showBurnConfirm = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("menu_burn_data_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Burn All Tabs & Data", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showBurnConfirm) {
        AlertDialog(
            onDismissRequest = { showBurnConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Burn Tabs & Data?") },
            text = { Text("Closes all open tabs and permanently wipes browsing history, cookies, and web cache.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBurnConfirm = false
                        onDismiss()
                        onBurnSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Burn Now") }
            },
            dismissButton = {
                TextButton(onClick = { showBurnConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCategoryPicker) {
        var newCategoryName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(TabGroupColor.COBALT) }
        val presets = listOf("Work", "Research", "Shopping", "Reading", "Social", "Travel")

        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CreateNewFolder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Assign Tab Category") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tabGroups.isNotEmpty()) {
                        Text(
                            text = "Existing Categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        tabGroups.forEach { group ->
                            val isAssigned = activeTabGroup?.id == group.id
                            Surface(
                                onClick = {
                                    onAssignTabToGroup(group.id)
                                    showCategoryPicker = false
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAssigned) {
                                    group.color.color.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                border = if (isAssigned) BorderStroke(1.dp, group.color.color) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(group.color.color)
                                    )
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAssigned) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Assigned",
                                            tint = group.color.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeTabGroup != null) {
                        OutlinedButton(
                            onClick = {
                                onAssignTabToGroup(null)
                                showCategoryPicker = false
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Remove from \"${activeTabGroup.name}\"")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Create New Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("e.g., Work, Shopping, Reading") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_new_category_input")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = { newCategoryName = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabGroupColor.entries.forEach { colorOption ->
                            val isSelected = selectedColor == colorOption
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colorOption.color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else colorOption.color,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorOption }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = colorOption.label,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreateGroupForActiveTab(trimmed, selectedColor)
                            showCategoryPicker = false
                            onDismiss()
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                    modifier = Modifier.testTag("sheet_create_category_confirm")
                ) {
                    Text("Create & Assign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = false }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftShieldPanelSheet(
    tab: TabSession,
    preferences: BrowserPreferencesState,
    isSiteProtected: Boolean,
    activeRuleCount: Int,
    onDismiss: () -> Unit,
    onToggleSiteProtection: () -> Unit,
    onToggleCosmetic: (Boolean) -> Unit,
    onToggleStripParams: (Boolean) -> Unit,
    onTogglePopups: (Boolean) -> Unit,
    onToggleAnnoyances: (Boolean) -> Unit,
    onToggleTurboMode: (Boolean) -> Unit,
    onCycleImagePolicy: () -> Unit,
    onClearSiteData: () -> Unit,
    onOpenFilterSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with glowing shield emblem
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val shieldTint = if (isSiteProtected) ShieldEmerald else WarningAmber
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shieldTint.copy(alpha = 0.16f))
                        .border(1.dp, shieldTint.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = shieldTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SwiftShield Pro",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (tab.isSecure) Icons.Filled.Lock else Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (tab.isSecure) ShieldEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (tab.isStartPage) "Global protection active • $activeRuleCount rules"
                            else "${tab.displayHost} • ${if (tab.isSecure) "Encrypted HTTPS" else "Standard HTTP"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live Per-Tab Protection Breakdown Strip
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(value = "${tab.blockedCount}", label = "Ads/Trackers", isAccent = true)
                    MetricItem(value = "${tab.strippedParamsCount}", label = "Links Cleaned", isAccent = tab.strippedParamsCount > 0)
                    MetricItem(value = "${tab.popupsBlockedCount}", label = "Popups", isAccent = tab.popupsBlockedCount > 0)
                    MetricItem(
                        value = tab.loadTimeMs?.let { "${it} ms" } ?: "$activeRuleCount",
                        label = if (tab.loadTimeMs != null) "Speed" else "Rules",
                        isAccent = false
                    )
                }
            }

            // Per-Site Protection Switch
            if (!tab.isStartPage) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Ads & Trackers Here", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (isSiteProtected) "Shield is active on ${tab.displayHost}" else "Paused for this site",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSiteProtected,
                            onCheckedChange = { onToggleSiteProtection() },
                            modifier = Modifier.testTag("shield_site_toggle")
                        )
                    }
                }
            }

            // Multi-Layer Protection Controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    ShieldToggleItem(
                        title = "YouTube, Video & Cosmetic Ad Shield",
                        subtitle = "Block YouTube video ads, ad containers & anti-adblock popups",
                        checked = preferences.cosmeticBlockingEnabled,
                        onCheckedChange = onToggleCosmetic,
                        tag = "shield_panel_cosmetic"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ShieldToggleItem(
                        title = "Strip URL Tracking Params",
                        subtitle = "Remove utm_*, fbclid, gclid from links",
                        checked = preferences.stripTrackingParamsEnabled,
                        onCheckedChange = onToggleStripParams,
                        tag = "shield_panel_strip_params"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ShieldToggleItem(
                        title = "Block Popups & Ad Redirects",
                        subtitle = "Stop popunder tabs and ad hijacks",
                        checked = preferences.blockPopupsAndRedirects,
                        onCheckedChange = onTogglePopups,
                        tag = "shield_panel_popups"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ShieldToggleItem(
                        title = "Block Cookie Banners",
                        subtitle = "Hide GDPR consent walls & unlock scroll",
                        checked = preferences.blockAnnoyances,
                        onCheckedChange = onToggleAnnoyances,
                        tag = "shield_panel_annoyances"
                    )
                }
            }

            // Quick Engine Speed Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { onToggleTurboMode(!preferences.turboModeEnabled) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (preferences.turboModeEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).testTag("shield_turbo_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Turbo Engine", style = MaterialTheme.typography.labelLarge)
                            Text(
                                if (preferences.turboModeEnabled) "Active" else "Off",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Surface(
                    onClick = onCycleImagePolicy,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).testTag("shield_image_policy_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Images", style = MaterialTheme.typography.labelLarge)
                            Text(
                                when (preferences.imageLoadingPolicy) {
                                    ImageLoadingPolicy.ALWAYS -> "Always"
                                    ImageLoadingPolicy.LAZY_OPTIMIZED -> "Lazy Fast"
                                    ImageLoadingPolicy.BLOCK_IMAGES -> "Blocked"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!tab.isStartPage) {
                    OutlinedButton(
                        onClick = { onClearSiteData(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Site Data")
                    }
                }
                Button(
                    onClick = { onDismiss(); onOpenFilterSettings() },
                    modifier = Modifier.weight(1f).testTag("shield_manage_filters_button")
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Filter Lists")
                }
            }
        }
    }
}

@Composable
private fun ShieldToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    payload: ContextMenuPayload,
    onDismiss: () -> Unit,
    onOpenNewTab: (String) -> Unit,
    onOpenIncognito: (String) -> Unit,
    onDownloadUrl: (String, Boolean) -> Unit,
    onCopyUrl: (String) -> Unit,
    onShareUrl: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (payload.isImage) "Image Options" else "Link Options",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = payload.url,
                style = MonoCodeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            SheetMenuRow(Icons.Filled.OpenInNew, "Open in New Tab", onClick = { onOpenNewTab(payload.url); onDismiss() })
            SheetMenuRow(Icons.Filled.VisibilityOff, "Open in Incognito Tab", onClick = { onOpenIncognito(payload.url); onDismiss() })
            SheetMenuRow(
                icon = Icons.Filled.Download,
                label = if (payload.isImage) "Download Image" else "Download Link",
                onClick = { onDownloadUrl(payload.url, payload.isImage); onDismiss() }
            )
            SheetMenuRow(Icons.Filled.ContentCopy, "Copy Address", onClick = { onCopyUrl(payload.url); onDismiss() })
            SheetMenuRow(Icons.Filled.Share, "Share Link", onClick = { onShareUrl(payload.url); onDismiss() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabHistoryBottomSheet(
    entries: List<TabHistoryEntry>,
    onDismiss: () -> Unit,
    onSelectIndex: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text("Tab Navigation History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries, key = { it.index }) { entry ->
                    Surface(
                        onClick = { onSelectIndex(entry.index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (entry.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = entry.url,
                                style = MonoCodeStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            highlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        border = when {
            !enabled -> null
            highlighted -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        },
        modifier = modifier.height(74.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        }
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        highlighted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium
                ),
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SheetMenuRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    tag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MetricItem(value: String, label: String, isAccent: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isAccent) ShieldEmerald else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
