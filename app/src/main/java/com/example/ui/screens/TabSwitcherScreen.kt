package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.TabLayoutMode
import com.example.viewmodel.TabGroup
import com.example.viewmodel.TabGroupColor
import com.example.viewmodel.TabSession
import kotlin.math.roundToInt

/**
 * Power Tab Switcher with:
 * - Standard & Incognito mode segments
 * - Named Categories / Color-Coded Tab Groups with horizontal Category Filter Bar
 * - Prominent visual group membership badges & colored accent indicators on every tab card/row
 * - Long-press Drag-and-Drop reordering with haptic feedback + quick reorder controls
 * - Multi-Select Bulk Closing & Grouping
 * - Visual Thumbnail Grid and Compact List modes
 */
@Composable
fun TabSwitcherScreen(
    tabs: List<TabSession>,
    tabGroups: List<TabGroup>,
    activeTabId: String,
    isIncognitoSegment: Boolean,
    activeGroupFilterId: String?,
    layoutMode: TabLayoutMode,
    isMultiSelectMode: Boolean,
    selectedTabIds: Set<String>,
    onSegmentChange: (Boolean) -> Unit,
    onFilterGroupChange: (String?) -> Unit,
    onToggleLayoutMode: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateNewTab: (isIncognito: Boolean, groupId: String?) -> Unit,
    onReorderTab: (fromId: String, toId: String) -> Unit,
    onToggleMultiSelect: (Boolean) -> Unit,
    onToggleTabSelected: (String) -> Unit,
    onSelectAll: () -> Unit,
    onCloseSelected: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit,
    onCreateGroup: (name: String, color: TabGroupColor, tabIds: Set<String>, isIncognito: Boolean) -> Unit,
    onAssignTabToGroup: (tabId: String, groupId: String?) -> Unit,
    onRenameRecolorGroup: (groupId: String, name: String, color: TabGroupColor) -> Unit,
    onToggleGroupCollapsed: (groupId: String) -> Unit,
    onUngroupCategory: (groupId: String) -> Unit,
    onCloseGroup: (groupId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val segmentTabs = remember(tabs, isIncognitoSegment) {
        tabs.filter { it.isIncognito == isIncognitoSegment }.sortedBy { it.orderIndex }
    }
    val segmentGroups = remember(tabGroups, isIncognitoSegment) {
        tabGroups.filter { it.isIncognito == isIncognitoSegment }
    }
    val visibleGroups = remember(segmentGroups, activeGroupFilterId) {
        if (activeGroupFilterId == null) segmentGroups
        else segmentGroups.filter { it.id == activeGroupFilterId }
    }
    val ungroupedTabs = remember(segmentTabs, segmentGroups, activeGroupFilterId) {
        if (activeGroupFilterId != null) {
            emptyList()
        } else {
            val validGroupIds = segmentGroups.map { it.id }.toSet()
            segmentTabs.filter { it.groupId == null || it.groupId !in validGroupIds }
        }
    }
    val normalCount = remember(tabs) { tabs.count { !it.isIncognito } }
    val incognitoCount = remember(tabs) { tabs.count { it.isIncognito } }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var createGroupTargetTabIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingGroup by remember { mutableStateOf<TabGroup?>(null) }
    var moveTabTarget by remember { mutableStateOf<TabSession?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Standard / Incognito Segmented Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                SegmentTabPill(
                                    label = "Tabs ($normalCount)",
                                    selected = !isIncognitoSegment,
                                    onClick = { onSegmentChange(false) },
                                    tag = "segment_normal_tabs"
                                )
                                SegmentTabPill(
                                    label = "Incognito ($incognitoCount)",
                                    selected = isIncognitoSegment,
                                    onClick = { onSegmentChange(true) },
                                    tag = "segment_incognito_tabs"
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleLayoutMode,
                                modifier = Modifier.testTag("tab_layout_toggle")
                            ) {
                                Icon(
                                    imageVector = if (layoutMode == TabLayoutMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                    contentDescription = "Switch tab grid or list view"
                                )
                            }
                            IconButton(
                                onClick = { onToggleMultiSelect(!isMultiSelectMode) },
                                modifier = Modifier.testTag("tab_multiselect_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Checklist,
                                    contentDescription = "Select multiple tabs",
                                    tint = if (isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.testTag("tab_switcher_more")
                                ) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More tab options")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Create Category / Group") },
                                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            createGroupTargetTabIds = if (selectedTabIds.isNotEmpty()) {
                                                selectedTabIds
                                            } else {
                                                setOfNotNull(segmentTabs.firstOrNull { it.id == activeTabId }?.id ?: segmentTabs.firstOrNull()?.id)
                                            }
                                            showCreateGroupDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Close Other Tabs") },
                                        leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onCloseOthers()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isIncognitoSegment) "Close All Incognito" else "Close All Tabs") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.DeleteSweep,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onCloseAll()
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("tab_switcher_close")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close tab switcher")
                            }
                        }
                    }

                    // Named Categories Filter & Quick-Create Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryFilterPill(
                            label = "All",
                            count = segmentTabs.size,
                            accentColor = MaterialTheme.colorScheme.primary,
                            showDot = false,
                            selected = activeGroupFilterId == null,
                            tag = "category_filter_all",
                            onClick = { onFilterGroupChange(null) }
                        )

                        segmentGroups.forEach { group ->
                            val count = segmentTabs.count { it.groupId == group.id }
                            CategoryFilterPill(
                                label = group.name,
                                count = count,
                                accentColor = group.color.color,
                                showDot = true,
                                selected = activeGroupFilterId == group.id,
                                tag = "category_filter_${group.id}",
                                onClick = { onFilterGroupChange(group.id) }
                            )
                        }

                        // One-tap New Category pill
                        Surface(
                            onClick = {
                                createGroupTargetTabIds = if (selectedTabIds.isNotEmpty()) {
                                    selectedTabIds
                                } else {
                                    setOfNotNull(
                                        segmentTabs.firstOrNull { it.id == activeTabId && it.groupId == null }?.id
                                            ?: segmentTabs.firstOrNull { it.id == activeTabId }?.id
                                    )
                                }
                                showCreateGroupDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("tab_new_category_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CreateNewFolder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "New Category",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isMultiSelectMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSelectAll,
                            modifier = Modifier.testTag("bulk_select_all_button")
                        ) {
                            Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("All")
                        }
                        OutlinedButton(
                            onClick = {
                                createGroupTargetTabIds = selectedTabIds
                                showCreateGroupDialog = true
                            },
                            enabled = selectedTabIds.isNotEmpty(),
                            modifier = Modifier.weight(1f).testTag("bulk_group_button")
                        ) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Categorize (${selectedTabIds.size})")
                        }
                        Button(
                            onClick = onCloseSelected,
                            enabled = selectedTabIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("bulk_close_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Close (${selectedTabIds.size})")
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                ExtendedFloatingActionButton(
                    onClick = { onCreateNewTab(isIncognitoSegment, activeGroupFilterId) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = {
                        val activeGroupName = activeGroupFilterId?.let { id ->
                            segmentGroups.firstOrNull { it.id == id }?.name
                        }
                        Text(
                            when {
                                activeGroupName != null -> "New in $activeGroupName"
                                isIncognitoSegment -> "New Incognito"
                                else -> "New Tab"
                            }
                        )
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("new_tab_fab")
                )
            }
        }
    ) { innerPadding ->
        if (segmentTabs.isEmpty() && segmentGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = if (isIncognitoSegment) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (isIncognitoSegment) "No Incognito Tabs Open" else "No Open Tabs",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isIncognitoSegment) {
                            "Tap below to open a private tab that leaves no history or cookies behind."
                        } else {
                            "Start a new browsing session or organize tabs into named categories."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onCreateNewTab(isIncognitoSegment, null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isIncognitoSegment) "Open Incognito Tab" else "Open New Tab")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Named Color-Coded Collapsible Tab Categories / Groups
                visibleGroups.forEach { group ->
                    val groupTabs = segmentTabs.filter { it.groupId == group.id }
                    item(key = "group_header_${group.id}") {
                        TabGroupHeaderCard(
                            group = group,
                            tabCount = groupTabs.size,
                            onToggleCollapse = { onToggleGroupCollapsed(group.id) },
                            onAddTabInGroup = { onCreateNewTab(isIncognitoSegment, group.id) },
                            onEditGroup = { editingGroup = group },
                            onUngroup = { onUngroupCategory(group.id) },
                            onCloseGroup = { onCloseGroup(group.id) }
                        )
                    }
                    if (!group.isCollapsed) {
                        if (groupTabs.isEmpty()) {
                            item(key = "grp_empty_${group.id}") {
                                Surface(
                                    onClick = { onCreateNewTab(isIncognitoSegment, group.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = group.color.color.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, group.color.color.copy(alpha = 0.28f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = group.color.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "No tabs in ${group.name} yet — tap to open one, or tap the folder icon on any tab to move it here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (layoutMode == TabLayoutMode.GRID) {
                            val rows = groupTabs.chunked(2)
                            items(rows.size, key = { "grp_${group.id}_row_$it" }) { rowIdx ->
                                val pair = rows[rowIdx]
                                TabGridRow(
                                    pair = pair,
                                    allOrderedTabs = segmentTabs,
                                    activeTabId = activeTabId,
                                    group = group,
                                    isMultiSelectMode = isMultiSelectMode,
                                    selectedTabIds = selectedTabIds,
                                    onSelectTab = onSelectTab,
                                    onCloseTab = onCloseTab,
                                    onToggleSelect = onToggleTabSelected,
                                    onMoveToGroup = { moveTabTarget = it },
                                    onReorderTab = onReorderTab
                                )
                            }
                        } else {
                            items(groupTabs, key = { "grp_list_${it.id}" }) { tab ->
                                TabListRowCard(
                                    tab = tab,
                                    allOrderedTabs = segmentTabs,
                                    isActive = tab.id == activeTabId,
                                    group = group,
                                    isMultiSelectMode = isMultiSelectMode,
                                    isSelected = tab.id in selectedTabIds,
                                    onSelect = { onSelectTab(tab.id) },
                                    onClose = { onCloseTab(tab.id) },
                                    onToggleSelect = { onToggleTabSelected(tab.id) },
                                    onMoveToGroup = { moveTabTarget = tab },
                                    onReorderTab = onReorderTab
                                )
                            }
                        }
                    }
                }

                // 2. Ungrouped Tabs (shown when no category filter is active)
                if (ungroupedTabs.isNotEmpty() && visibleGroups.isNotEmpty()) {
                    item(key = "ungrouped_header") {
                        Text(
                            text = "Ungrouped Tabs (${ungroupedTabs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }
                }

                if (layoutMode == TabLayoutMode.GRID) {
                    val rows = ungroupedTabs.chunked(2)
                    items(rows.size, key = { "ungrouped_row_$it" }) { rowIdx ->
                        TabGridRow(
                            pair = rows[rowIdx],
                            allOrderedTabs = segmentTabs,
                            activeTabId = activeTabId,
                            group = null,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedTabIds = selectedTabIds,
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                            onToggleSelect = onToggleTabSelected,
                            onMoveToGroup = { moveTabTarget = it },
                            onReorderTab = onReorderTab
                        )
                    }
                } else {
                    items(ungroupedTabs, key = { "list_${it.id}" }) { tab ->
                        TabListRowCard(
                            tab = tab,
                            allOrderedTabs = segmentTabs,
                            isActive = tab.id == activeTabId,
                            group = null,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = tab.id in selectedTabIds,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) },
                            onToggleSelect = { onToggleTabSelected(tab.id) },
                            onMoveToGroup = { moveTabTarget = tab },
                            onReorderTab = onReorderTab
                        )
                    }
                }
            }
        }
    }

    // Create New Named Category / Group Dialog
    if (showCreateGroupDialog) {
        TabGroupEditorDialog(
            title = "Create Tab Category",
            initialName = "",
            initialColor = TabGroupColor.COBALT,
            onDismiss = { showCreateGroupDialog = false },
            onSave = { name, color ->
                onCreateGroup(name, color, createGroupTargetTabIds, isIncognitoSegment)
                showCreateGroupDialog = false
            }
        )
    }

    // Rename / Recolor Existing Category Dialog
    editingGroup?.let { group ->
        TabGroupEditorDialog(
            title = "Edit Category",
            initialName = group.name,
            initialColor = group.color,
            onDismiss = { editingGroup = null },
            onSave = { name, color ->
                onRenameRecolorGroup(group.id, name, color)
                editingGroup = null
            }
        )
    }

    // Assign Tab to Category Dialog
    moveTabTarget?.let { tab ->
        AlertDialog(
            onDismissRequest = { moveTabTarget = null },
            title = { Text("Organize Tab Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tab.title.ifBlank { tab.displayHost },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        onClick = {
                            onAssignTabToGroup(tab.id, null)
                            moveTabTarget = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (tab.groupId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ungrouped", style = MaterialTheme.typography.bodyMedium)
                            if (tab.groupId == null) {
                                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    segmentGroups.forEach { group ->
                        val isMember = tab.groupId == group.id
                        Surface(
                            onClick = {
                                onAssignTabToGroup(tab.id, group.id)
                                moveTabTarget = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = group.color.color.copy(alpha = if (isMember) 0.24f else 0.13f),
                            border = if (isMember) BorderStroke(1.5.dp, group.color.color) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(group.color.color)
                                )
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isMember) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isMember) {
                                    Text("Current", style = MaterialTheme.typography.labelSmall, color = group.color.color)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            createGroupTargetTabIds = setOf(tab.id)
                            moveTabTarget = null
                            showCreateGroupDialog = true
                        },
                        modifier = Modifier.testTag("dialog_create_new_category_button")
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Create New Category")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moveTabTarget = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun CategoryFilterPill(
    label: String,
    count: Int,
    accentColor: Color,
    showDot: Boolean,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SegmentTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(),
        label = "segment_bg_$tag"
    )
    val fgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "segment_fg_$tag"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = bgColor,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TabGroupHeaderCard(
    group: TabGroup,
    tabCount: Int,
    onToggleCollapse: () -> Unit,
    onAddTabInGroup: () -> Unit,
    onEditGroup: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = group.color.color.copy(alpha = 0.14f),
        border = BorderStroke(1.2.dp, group.color.color.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_group_header_${group.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapse)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(group.color.color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = CircleShape,
                color = group.color.color.copy(alpha = 0.25f)
            ) {
                Text(
                    text = "$tabCount",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            IconButton(onClick = onAddTabInGroup, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Add tab to category", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEditGroup, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename or recolor category", modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onUngroup, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.FolderOff, contentDescription = "Ungroup category and keep tabs open", modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onCloseGroup, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close all tabs in category", modifier = Modifier.size(18.dp))
            }
            Icon(
                imageVector = if (group.isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (group.isCollapsed) "Expand category" else "Collapse category"
            )
        }
    }
}

@Composable
private fun TabGridRow(
    pair: List<TabSession>,
    allOrderedTabs: List<TabSession>,
    activeTabId: String,
    group: TabGroup?,
    isMultiSelectMode: Boolean,
    selectedTabIds: Set<String>,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onMoveToGroup: (TabSession) -> Unit,
    onReorderTab: (String, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pair.forEach { tab ->
            TabGridCard(
                tab = tab,
                allOrderedTabs = allOrderedTabs,
                isActive = tab.id == activeTabId,
                group = group,
                isMultiSelectMode = isMultiSelectMode,
                isSelected = tab.id in selectedTabIds,
                onClick = {
                    if (isMultiSelectMode) onToggleSelect(tab.id) else onSelectTab(tab.id)
                },
                onClose = { onCloseTab(tab.id) },
                onMoveToGroup = { onMoveToGroup(tab) },
                onReorderTab = onReorderTab,
                modifier = Modifier.weight(1f)
            )
        }
        if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabGridCard(
    tab: TabSession,
    allOrderedTabs: List<TabSession>,
    isActive: Boolean,
    group: TabGroup?,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onMoveToGroup: () -> Unit,
    onReorderTab: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val groupAccent = group?.color?.color
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isActive -> groupAccent ?: MaterialTheme.colorScheme.primary
        groupAccent != null -> groupAccent.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDragging) 10.dp else 2.dp,
        shadowElevation = if (isDragging) 14.dp else 1.dp,
        border = BorderStroke(if (isActive || isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .height(216.dp)
            .zIndex(if (isDragging) 10f else 0f)
            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
            .pointerInput(tab.id, allOrderedTabs) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val currentIdx = allOrderedTabs.indexOfFirst { it.id == tab.id }
                        if (currentIdx != -1) {
                            val stepDelta = when {
                                dragOffsetY > 110f -> 2
                                dragOffsetY < -110f -> -2
                                dragOffsetX > 90f -> 1
                                dragOffsetX < -90f -> -1
                                else -> 0
                            }
                            val targetIdx = (currentIdx + stepDelta).coerceIn(0, allOrderedTabs.lastIndex)
                            if (targetIdx != currentIdx) {
                                onReorderTab(tab.id, allOrderedTabs[targetIdx].id)
                            }
                        }
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onDragCancel = {
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                        dragOffsetY += dragAmount.y
                    }
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("tab_card_${tab.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Category Color Accent Band (Visual Group Membership Indicator)
            if (groupAccent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(groupAccent)
                )
            }

            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isActive -> (groupAccent ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.16f)
                            groupAccent != null -> groupAccent.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = "Select tab",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Long press to drag",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }

                Text(
                    text = tab.title.ifBlank { tab.displayHost },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!isMultiSelectMode) {
                    IconButton(
                        onClick = onMoveToGroup,
                        modifier = Modifier.size(28.dp).testTag("tab_assign_group_${tab.id}")
                    ) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = "Assign tab to category",
                            tint = groupAccent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp).testTag("close_tab_${tab.id}")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close tab",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Live Thumbnail Preview or Clean Domain Placeholder + Floating Group Membership Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                val bmp = tab.thumbnail
                if (bmp != null && !tab.isStartPage) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Tab preview for ${tab.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                            contentDescription = null,
                            tint = (groupAccent ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.78f),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = tab.displayHost,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Visual Group Membership Pill Badge overlaid at bottom-left
                if (group != null) {
                    GroupMembershipBadge(
                        group = group,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .testTag("tab_group_badge_${tab.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun TabListRowCard(
    tab: TabSession,
    allOrderedTabs: List<TabSession>,
    isActive: Boolean,
    group: TabGroup?,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onToggleSelect: () -> Unit,
    onMoveToGroup: () -> Unit,
    onReorderTab: (String, String) -> Unit
) {
    val idx = allOrderedTabs.indexOfFirst { it.id == tab.id }
    val groupAccent = group?.color?.color

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) (groupAccent ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive || isSelected) 1.5.dp else 1.dp,
            color = when {
                isActive || isSelected -> groupAccent ?: MaterialTheme.colorScheme.primary
                groupAccent != null -> groupAccent.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (isMultiSelectMode) onToggleSelect() else onSelect() }
            .testTag("tab_list_row_${tab.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = "Select tab",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (groupAccent != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(groupAccent)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tab.title.ifBlank { tab.displayHost },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (group != null) {
                        GroupMembershipBadge(
                            group = group,
                            modifier = Modifier.testTag("tab_list_group_badge_${tab.id}")
                        )
                    }
                    Text(
                        text = tab.displayHost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isMultiSelectMode) {
                if (idx > 0) {
                    IconButton(
                        onClick = { onReorderTab(tab.id, allOrderedTabs[idx - 1].id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Move tab up", modifier = Modifier.size(16.dp))
                    }
                }
                if (idx in 0 until allOrderedTabs.lastIndex) {
                    IconButton(
                        onClick = { onReorderTab(tab.id, allOrderedTabs[idx + 1].id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Move tab down", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onMoveToGroup, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Move to category",
                        tint = groupAccent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close tab", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupMembershipBadge(
    group: TabGroup,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, group.color.color.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(group.color.color)
            )
            Text(
                text = group.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TabGroupEditorDialog(
    title: String,
    initialName: String,
    initialColor: TabGroupColor,
    onDismiss: () -> Unit,
    onSave: (String, TabGroupColor) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var chosenColor by remember { mutableStateOf(initialColor) }
    val presetCategories = remember { listOf("Work", "Research", "Shopping", "Reading", "Social", "Travel") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Research, Work, Shopping") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { preset ->
                        AssistChip(
                            onClick = { name = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Category Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabGroupColor.entries.forEach { preset ->
                        val selected = preset == chosenColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { chosenColor = preset }
                                .testTag("category_color_${preset.name.lowercase()}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.ifBlank { "Research" }, chosenColor) },
                modifier = Modifier.testTag("category_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
