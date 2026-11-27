package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BookmarkEntity
import com.example.data.DownloadEntity
import com.example.data.DownloadStatus
import com.example.data.HistoryEntity
import com.example.ui.components.FavoriteSiteIcon
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.viewmodel.LibraryTab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class HistoryRangeFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    OLDER("Older")
}

private enum class ClearTimeRange(val label: String, val durationMillis: Long?) {
    LAST_HOUR("1 Hour", 60L * 60L * 1000L),
    LAST_24H("24 Hours", 24L * 60L * 60L * 1000L),
    LAST_7D("7 Days", 7L * 24L * 60L * 60L * 1000L),
    ALL_TIME("All Time", null)
}

private enum class HistoryBucket(val title: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("Earlier This Week"),
    OLDER("Older")
}

@Composable
fun LibraryAndDownloadsScreen(
    activeTab: LibraryTab,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    downloads: List<DownloadEntity>,
    downloadSpeedsById: Map<Long, Long>,
    onTabChange: (LibraryTab) -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddBookmark: (title: String, url: String, folder: String, isSpeedDial: Boolean) -> Unit,
    onToggleSpeedDial: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearBrowsingData: (clearHistory: Boolean, clearCache: Boolean, clearCookies: Boolean, sinceMillis: Long) -> Unit,
    onStartManualDownload: (String) -> Unit,
    onPauseDownload: (Long) -> Unit,
    onResumeDownload: (Long) -> Unit,
    onCancelDownload: (Long) -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onClearDownloads: () -> Unit,
    onOpenDownloadedFile: (DownloadEntity) -> Unit,
    onShareDownloadedFile: (DownloadEntity) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var historyRangeFilter by remember { mutableStateOf(HistoryRangeFilter.ALL) }
    var selectedBookmarkFolder by remember { mutableStateOf("All") }

    val activeDownloadCount = remember(downloads) {
        downloads.count { it.downloadStatus == DownloadStatus.DOWNLOADING }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("library_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Close library"
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = when (activeTab) {
                                    LibraryTab.BOOKMARKS -> "Bookmarks"
                                    LibraryTab.HISTORY -> "History"
                                    LibraryTab.DOWNLOADS -> "Downloads"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = when (activeTab) {
                                LibraryTab.BOOKMARKS -> "${bookmarks.size} saved ${if (bookmarks.size == 1) "page" else "pages"}"
                                LibraryTab.HISTORY -> "${history.size} visited ${if (history.size == 1) "entry" else "entries"}"
                                LibraryTab.DOWNLOADS -> if (activeDownloadCount > 0) {
                                    "${downloads.size} files • $activeDownloadCount active"
                                } else {
                                    "${downloads.size} saved ${if (downloads.size == 1) "file" else "files"}"
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        when (activeTab) {
                            LibraryTab.BOOKMARKS -> {
                                FilledTonalIconButton(
                                    onClick = { showAddBookmarkDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("library_add_bookmark")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add bookmark")
                                }
                            }
                            LibraryTab.HISTORY -> {
                                FilledTonalIconButton(
                                    onClick = { showClearDataDialog = true },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("library_clear_history")
                                ) {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear browsing data")
                                }
                            }
                            LibraryTab.DOWNLOADS -> {
                                if (downloads.isNotEmpty()) {
                                    FilledTonalIconButton(
                                        onClick = onClearDownloads,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear download log")
                                    }
                                }
                            }
                        }
                    }

                    // Luminous Segmented Pill Tab Switcher
                    LibrarySegmentedTabBar(
                        activeTab = activeTab,
                        bookmarksCount = bookmarks.size,
                        historyCount = history.size,
                        downloadsCount = downloads.size,
                        hasActiveDownloads = activeDownloadCount > 0,
                        onTabChange = onTabChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    // Sleek Pill Search Capsule
                    LibrarySearchCapsule(
                        query = searchQuery,
                        placeholder = when (activeTab) {
                            LibraryTab.BOOKMARKS -> "Search bookmarks & folders..."
                            LibraryTab.HISTORY -> "Search browsing history..."
                            LibraryTab.DOWNLOADS -> "Search files, APKs, or URLs..."
                        },
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                when (activeTab) {
                    LibraryTab.BOOKMARKS -> {
                        BookmarksTabContent(
                            bookmarks = bookmarks,
                            searchQuery = searchQuery,
                            selectedFolder = selectedBookmarkFolder,
                            onSelectFolder = { selectedBookmarkFolder = it },
                            onOpenUrl = onOpenUrl,
                            onToggleSpeedDial = onToggleSpeedDial,
                            onDeleteBookmark = onDeleteBookmark,
                            onAddBookmarkClick = { showAddBookmarkDialog = true }
                        )
                    }

                    LibraryTab.HISTORY -> {
                        HistoryTabContent(
                            history = history,
                            searchQuery = searchQuery,
                            selectedRange = historyRangeFilter,
                            onSelectRange = { historyRangeFilter = it },
                            onOpenUrl = onOpenUrl,
                            onDeleteHistoryItem = onDeleteHistoryItem,
                            onOpenClearDialog = { showClearDataDialog = true }
                        )
                    }

                    LibraryTab.DOWNLOADS -> {
                        DownloadsManagerContent(
                            downloads = downloads,
                            speedsById = downloadSpeedsById,
                            searchQuery = searchQuery,
                            onStartManualDownload = onStartManualDownload,
                            onPauseDownload = onPauseDownload,
                            onResumeDownload = onResumeDownload,
                            onCancelDownload = onCancelDownload,
                            onDeleteDownload = onDeleteDownload,
                            onOpenFile = onOpenDownloadedFile,
                            onShareFile = onShareDownloadedFile
                        )
                    }
                }
            }
        }
    }

    if (showAddBookmarkDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var folder by remember { mutableStateOf("General") }
        var pinDial by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("Folder") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pinDial = !pinDial }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(checked = pinDial, onCheckedChange = { pinDial = it })
                        Text("Pin to Start Page Speed Dial", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBookmark(title.ifBlank { url }, url, folder.ifBlank { "General" }, pinDial)
                        showAddBookmarkDialog = false
                    },
                    enabled = url.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDataDialog) {
        var clearHist by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(false) }
        var selectedTimeRange by remember { mutableStateOf(ClearTimeRange.ALL_TIME) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Browsing Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Time Range",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClearTimeRange.entries.forEach { range ->
                            val isSelected = selectedTimeRange == range
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedTimeRange = range }
                            ) {
                                Text(
                                    text = range.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    ClearOptionRow(
                        checked = clearHist,
                        title = "Browsing History",
                        subtitle = "Removes visited URLs and timestamps",
                        onToggle = { clearHist = !clearHist }
                    )
                    ClearOptionRow(
                        checked = clearCache,
                        title = "Cached Images & Files",
                        subtitle = "Frees temporary page resources",
                        onToggle = { clearCache = !clearCache }
                    )
                    ClearOptionRow(
                        checked = clearCookies,
                        title = "Cookies & Site Data",
                        subtitle = "Signs you out of most websites",
                        onToggle = { clearCookies = !clearCookies }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val since = selectedTimeRange.durationMillis?.let {
                            (System.currentTimeMillis() - it).coerceAtLeast(0L)
                        } ?: 0L
                        onClearBrowsingData(clearHist, clearCache, clearCookies, since)
                        showClearDataDialog = false
                    },
                    enabled = clearHist || clearCache || clearCookies
                ) { Text("Clear Data") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ClearOptionRow(
    checked: Boolean,
    title: String,
    subtitle: String,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibrarySegmentedTabBar(
    activeTab: LibraryTab,
    bookmarksCount: Int,
    historyCount: Int,
    downloadsCount: Int,
    hasActiveDownloads: Boolean,
    onTabChange: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LibraryTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val bgColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_bg_${tab.name}"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_fg_${tab.name}"
                )
                val (label, icon, count) = when (tab) {
                    LibraryTab.BOOKMARKS -> Triple("Bookmarks", Icons.Rounded.Bookmarks, bookmarksCount)
                    LibraryTab.HISTORY -> Triple("History", Icons.Rounded.History, historyCount)
                    LibraryTab.DOWNLOADS -> Triple("Downloads", Icons.Rounded.Download, downloadsCount)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor,
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTabChange(tab) }
                        .testTag("library_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(17.dp)
                            )
                            if (tab == LibraryTab.DOWNLOADS && hasActiveDownloads) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(ShieldEmerald)
                                )
                            }
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (count > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor.copy(alpha = 0.78f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchCapsule(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    history: List<HistoryEntity>,
    searchQuery: String,
    selectedRange: HistoryRangeFilter,
    onSelectRange: (HistoryRangeFilter) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onOpenClearDialog: () -> Unit
) {
    val boundaries = remember(history.size) { computeHistoryDayBoundaries() }
    val uniqueSitesCount = remember(history) {
        history.mapNotNull { extractCleanHost(it.url).ifBlank { null } }.distinct().size
    }

    val filteredHistory = remember(history, searchQuery, selectedRange, boundaries) {
        val bySearch = if (searchQuery.isBlank()) history else history.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
        when (selectedRange) {
            HistoryRangeFilter.ALL -> bySearch
            HistoryRangeFilter.TODAY -> bySearch.filter { it.visitedAt >= boundaries.todayStart }
            HistoryRangeFilter.YESTERDAY -> bySearch.filter {
                it.visitedAt in boundaries.yesterdayStart until boundaries.todayStart
            }
            HistoryRangeFilter.THIS_WEEK -> bySearch.filter {
                it.visitedAt in boundaries.weekStart until boundaries.yesterdayStart
            }
            HistoryRangeFilter.OLDER -> bySearch.filter { it.visitedAt < boundaries.weekStart }
        }
    }

    val groupedHistory = remember(filteredHistory, boundaries) {
        val buckets = linkedMapOf<HistoryBucket, MutableList<HistoryEntity>>()
        filteredHistory.forEach { item ->
            val bucket = when {
                item.visitedAt >= boundaries.todayStart -> HistoryBucket.TODAY
                item.visitedAt >= boundaries.yesterdayStart -> HistoryBucket.YESTERDAY
                item.visitedAt >= boundaries.weekStart -> HistoryBucket.THIS_WEEK
                else -> HistoryBucket.OLDER
            }
            buckets.getOrPut(bucket) { mutableListOf() }.add(item)
        }
        buckets.toList()
    }

    val timeOnlyFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateTimeFmt = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Luminous History Overview Card + Time Filter Strip
        HistoryHeroCard(
            totalVisits = history.size,
            uniqueSites = uniqueSitesCount,
            onClearClick = onOpenClearDialog,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryRangeFilter.entries.forEach { range ->
                val isSelected = selectedRange == range
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectRange(range) }
                        .testTag("history_filter_${range.name.lowercase()}")
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            EmptyLibraryState(
                icon = Icons.Rounded.History,
                title = if (history.isEmpty()) "No Browsing History" else "No Matching Entries",
                subtitle = if (history.isEmpty()) {
                    "Pages you visit in standard tabs appear here on your device. Incognito tabs are never recorded."
                } else {
                    "Try a different search keyword or switch the time range filter."
                },
                accentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedHistory.forEach { (bucket, entries) ->
                    item(key = "history_header_${bucket.name}") {
                        HistorySectionHeader(
                            title = bucket.title,
                            count = entries.size,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(entries, key = { "hist_${it.id}" }) { item ->
                        val timeText = if (bucket == HistoryBucket.TODAY || bucket == HistoryBucket.YESTERDAY) {
                            timeOnlyFmt.format(Date(item.visitedAt))
                        } else {
                            dateTimeFmt.format(Date(item.visitedAt))
                        }
                        HistoryEntryCard(
                            item = item,
                            formattedTime = timeText,
                            onClick = { onOpenUrl(item.url) },
                            onDelete = { onDeleteHistoryItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeroCard(
    totalVisits: Int,
    uniqueSites: Int,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColor = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.2f),
                            radius = size.maxDimension * 0.55f
                        )
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (totalVisits == 0) "Clean Timeline" else "$totalVisits ${if (totalVisits == 1) "Page" else "Pages"} Visited",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uniqueSites == 0) {
                            "Stored locally • Private tabs excluded"
                        } else {
                            "$uniqueSites unique ${if (uniqueSites == 1) "site" else "sites"} • Local only"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onClearClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Clear", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    item: HistoryEntity,
    formattedTime: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val host = remember(item.url) { extractCleanHost(item.url).ifBlank { item.url } }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("history_entry_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FavoriteSiteIcon(
                title = item.title,
                url = item.url,
                size = 38.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title.ifBlank { host },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = host,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("history_delete_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete history entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(
    bookmarks: List<BookmarkEntity>,
    searchQuery: String,
    selectedFolder: String,
    onSelectFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleSpeedDial: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddBookmarkClick: () -> Unit
) {
    val folders = remember(bookmarks) {
        val distinct = bookmarks.map { it.folder.ifBlank { "General" } }.distinct().sorted()
        if (distinct.size > 1) listOf("All") + distinct else emptyList()
    }
    val effectiveFolder = if (selectedFolder in folders || selectedFolder == "All") selectedFolder else "All"

    val filtered = remember(bookmarks, searchQuery, effectiveFolder) {
        bookmarks.filter { bm ->
            val matchesFolder = effectiveFolder == "All" || bm.folder.ifBlank { "General" } == effectiveFolder
            val matchesSearch = searchQuery.isBlank() ||
                bm.title.contains(searchQuery, ignoreCase = true) ||
                bm.url.contains(searchQuery, ignoreCase = true) ||
                bm.folder.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (folders.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                folders.forEach { folder ->
                    val isSelected = effectiveFolder == folder
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectFolder(folder) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                        ) {
                            if (folder != "All") {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyLibraryState(
                icon = Icons.Rounded.Bookmarks,
                title = if (bookmarks.isEmpty()) "No Bookmarks Yet" else "No Matching Bookmarks",
                subtitle = if (bookmarks.isEmpty()) {
                    "Save your favorite pages or pin them to your Start Page Speed Dial."
                } else {
                    "Try clearing your search query or switching folders."
                },
                accentColor = MaterialTheme.colorScheme.primary,
                ctaLabel = if (bookmarks.isEmpty()) "Add Bookmark" else null,
                onCtaClick = if (bookmarks.isEmpty()) onAddBookmarkClick else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { "bm_${it.id}" }) { bm ->
                    val host = remember(bm.url) { extractCleanHost(bm.url).ifBlank { bm.url } }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenUrl(bm.url) }
                            .testTag("bookmark_card_${bm.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FavoriteSiteIcon(
                                title = bm.title,
                                url = bm.url,
                                size = 38.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = bm.title.ifBlank { host },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = bm.folder.ifBlank { "General" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = host,
                                        style = MonoCodeStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onToggleSpeedDial(bm) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (bm.isSpeedDial) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (bm.isSpeedDial) "Unpin from Speed Dial" else "Pin to Speed Dial",
                                    tint = if (bm.isSpeedDial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteBookmark(bm.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete bookmark",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyLibraryState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    accentColor.copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.65f
                            )
                        )
                    }
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (ctaLabel != null && onCtaClick != null) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onCtaClick,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(ctaLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private data class HistoryDayBoundaries(
    val todayStart: Long,
    val yesterdayStart: Long,
    val weekStart: Long
)

private fun computeHistoryDayBoundaries(): HistoryDayBoundaries {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStart = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val weekStart = cal.timeInMillis
    return HistoryDayBoundaries(
        todayStart = todayStart,
        yesterdayStart = yesterdayStart,
        weekStart = weekStart
    )
}

internal fun extractCleanHost(url: String): String {
    return runCatching {
        Uri.parse(url).host?.removePrefix("www.")
    }.getOrNull().orEmpty()
}
