package com.example.ui.screens

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BookmarkEntity
import com.example.data.DownloadEntity
import com.example.data.HistoryEntity
import com.example.ui.theme.MonoCodeStyle
import com.example.viewmodel.LibraryTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.testTag("library_close_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close library")
                        }
                        Text("Library", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        when (activeTab) {
                            LibraryTab.BOOKMARKS -> {
                                IconButton(
                                    onClick = { showAddBookmarkDialog = true },
                                    modifier = Modifier.testTag("library_add_bookmark")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add bookmark")
                                }
                            }
                            LibraryTab.HISTORY -> {
                                IconButton(
                                    onClick = { showClearDataDialog = true },
                                    modifier = Modifier.testTag("library_clear_history")
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear browsing data")
                                }
                            }
                            LibraryTab.DOWNLOADS -> {
                                if (downloads.isNotEmpty()) {
                                    IconButton(onClick = onClearDownloads) {
                                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear download log")
                                    }
                                }
                            }
                        }
                    }

                    TabRow(selectedTabIndex = activeTab.ordinal) {
                        LibraryTab.entries.forEach { tab ->
                            Tab(
                                selected = activeTab == tab,
                                onClick = { onTabChange(tab) },
                                text = {
                                    Text(
                                        when (tab) {
                                            LibraryTab.BOOKMARKS -> "Bookmarks"
                                            LibraryTab.HISTORY -> "History"
                                            LibraryTab.DOWNLOADS -> "Downloads"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter ${activeTab.name.lowercase()}...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when (activeTab) {
                LibraryTab.BOOKMARKS -> {
                    val filtered = remember(bookmarks, searchQuery) {
                        if (searchQuery.isBlank()) bookmarks
                        else bookmarks.filter {
                            it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true) || it.folder.contains(searchQuery, true)
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyLibraryState(Icons.Filled.Bookmark, "No Bookmarks Yet", "Save your favorite pages or pin them to Speed Dial.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.id }) { bm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenUrl(bm.url) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bm.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${bm.folder} • ${bm.url}", style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onToggleSpeedDial(bm) }) {
                                        Icon(
                                            imageVector = if (bm.isSpeedDial) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = if (bm.isSpeedDial) "Unpin from Speed Dial" else "Pin to Speed Dial",
                                            tint = if (bm.isSpeedDial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete bookmark")
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }

                LibraryTab.HISTORY -> {
                    val filtered = remember(history, searchQuery) {
                        if (searchQuery.isBlank()) history
                        else history.filter { it.title.contains(searchQuery, true) || it.url.contains(searchQuery, true) }
                    }
                    if (filtered.isEmpty()) {
                        EmptyLibraryState(Icons.Filled.History, "No Browsing History", "Pages you visit in standard tabs appear here locally.")
                    } else {
                        val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenUrl(item.url) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${fmt.format(Date(item.visitedAt))} • ${item.url}",
                                            style = MonoCodeStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteHistoryItem(item.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete history entry")
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
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

    if (showAddBookmarkDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var folder by remember { mutableStateOf("General") }
        var pinDial by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
                    OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("Folder") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinDial, onCheckedChange = { pinDial = it })
                        Text("Pin to Speed Dial")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBookmark(title.ifBlank { url }, url, folder, pinDial)
                        showAddBookmarkDialog = false
                    },
                    enabled = url.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddBookmarkDialog = false }) { Text("Cancel") } }
        )
    }

    if (showClearDataDialog) {
        var clearHist by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Browsing Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearHist, onCheckedChange = { clearHist = it })
                        Text("Browsing History")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                        Text("Cached Images & Files")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                        Text("Cookies & Site Data")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBrowsingData(clearHist, clearCache, clearCookies, 0L)
                        showClearDataDialog = false
                    }
                ) { Text("Clear Data") }
            },
            dismissButton = { TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(52.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
