package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DownloadEntity
import com.example.data.DownloadStatus
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DownloadFilter(val label: String) {
    ALL("All"),
    APPS("Apps (APK)"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

internal fun isApkDownload(fileName: String, mimeType: String): Boolean {
    val name = fileName.lowercase()
    val mime = mimeType.lowercase()
    return name.endsWith(".apk") ||
        name.endsWith(".apks") ||
        name.endsWith(".xapk") ||
        name.endsWith(".aab") ||
        mime.contains("android.package-archive")
}

@Composable
fun DownloadsManagerContent(
    downloads: List<DownloadEntity>,
    speedsById: Map<Long, Long>,
    searchQuery: String,
    onStartManualDownload: (String) -> Unit,
    onPauseDownload: (Long) -> Unit,
    onResumeDownload: (Long) -> Unit,
    onCancelDownload: (Long) -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onOpenFile: (DownloadEntity) -> Unit,
    onShareFile: (DownloadEntity) -> Unit
) {
    var showNewDownloadDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filtered = remember(downloads, searchQuery, selectedFilter) {
        val byQuery = if (searchQuery.isBlank()) downloads
        else downloads.filter {
            it.fileName.contains(searchQuery, true) ||
                it.url.contains(searchQuery, true) ||
                it.mimeType.contains(searchQuery, true)
        }
        when (selectedFilter) {
            DownloadFilter.ALL -> byQuery
            DownloadFilter.APPS -> byQuery.filter { isApkDownload(it.fileName, it.mimeType) }
            DownloadFilter.ACTIVE -> byQuery.filter {
                it.downloadStatus == DownloadStatus.DOWNLOADING || it.downloadStatus == DownloadStatus.PAUSED
            }
            DownloadFilter.COMPLETED -> byQuery.filter { it.downloadStatus == DownloadStatus.COMPLETED }
        }
    }

    val activeTransfers = remember(filtered) {
        filtered.filter {
            it.downloadStatus == DownloadStatus.DOWNLOADING ||
                it.downloadStatus == DownloadStatus.PAUSED ||
                it.downloadStatus == DownloadStatus.FAILED ||
                it.downloadStatus == DownloadStatus.CANCELLED
        }
    }
    val completedFiles = remember(filtered) {
        filtered.filter { it.downloadStatus == DownloadStatus.COMPLETED }
    }

    val apkCount = remember(downloads) { downloads.count { isApkDownload(it.fileName, it.mimeType) } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary & Quick Manual Download Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val activeCount = downloads.count { it.downloadStatus == DownloadStatus.DOWNLOADING }
                    val pausedCount = downloads.count { it.downloadStatus == DownloadStatus.PAUSED }
                    val doneCount = downloads.count { it.downloadStatus == DownloadStatus.COMPLETED }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                activeCount > 0 -> "$activeCount downloading${if (pausedCount > 0) " • $pausedCount paused" else ""}"
                                pausedCount > 0 -> "$pausedCount paused"
                                else -> "$doneCount downloaded • $apkCount app(s)"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Tracks APK app packages & files • Pause/Resume supported",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { showNewDownloadDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("downloads_add_url_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download URL", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Filter Chips (All / Apps (APK) / Active / Completed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DownloadFilter.entries.forEach { filter ->
                        val chipLabel = if (filter == DownloadFilter.APPS && apkCount > 0) {
                            "${filter.label} ($apkCount)"
                        } else {
                            filter.label
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(chipLabel, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("downloads_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text("No Downloads Yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Tap any download link on a page, long-press an image, or paste a direct file URL to start downloading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { showNewDownloadDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download from URL")
                    }
                }
            }
        } else {
            val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTransfers.isNotEmpty()) {
                    item(key = "section_active") {
                        Text(
                            text = "Active & Queued Transfers (${activeTransfers.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        )
                    }
                    items(activeTransfers, key = { "active_${it.id}" }) { item ->
                        ActiveDownloadCard(
                            item = item,
                            speedBytesPerSec = speedsById[item.id] ?: 0L,
                            onPause = { onPauseDownload(item.id) },
                            onResume = { onResumeDownload(item.id) },
                            onCancel = { onCancelDownload(item.id) },
                            onDelete = { onDeleteDownload(item.id) }
                        )
                    }
                }

                if (completedFiles.isNotEmpty()) {
                    item(key = "section_recent") {
                        Text(
                            text = "Recently Downloaded Files (${completedFiles.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp, top = 6.dp)
                        )
                    }
                    items(completedFiles, key = { "done_${it.id}" }) { item ->
                        CompletedDownloadCard(
                            item = item,
                            formattedTime = dateFmt.format(Date(item.timestamp)),
                            onOpen = { onOpenFile(item) },
                            onShare = { onShareFile(item) },
                            onDelete = { onDeleteDownload(item.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showNewDownloadDialog) {
        var urlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDownloadDialog = false },
            title = { Text("Download File from URL") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Direct File URL") },
                    placeholder = { Text("https://example.com/file.pdf") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_download_url_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartManualDownload(urlInput)
                        showNewDownloadDialog = false
                    },
                    enabled = urlInput.isNotBlank(),
                    modifier = Modifier.testTag("manual_download_start_button")
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDownloadDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActiveDownloadCard(
    item: DownloadEntity,
    speedBytesPerSec: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val status = item.downloadStatus
    val animatedProgress by animateFloatAsState(
        targetValue = item.progressFraction,
        label = "dl_progress_${item.id}"
    )

    val (statusLabel, statusColor) = when (status) {
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "Paused" to WarningAmber
        DownloadStatus.FAILED -> (item.errorMessage.ifBlank { "Failed" }) to DangerCrimson
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.COMPLETED -> "Completed" to ShieldEmerald
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_download_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = if (status == DownloadStatus.FAILED) Icons.Filled.ErrorOutline else iconForMime(item.mimeType, item.fileName),
                        contentDescription = null,
                        tint = statusColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isApkDownload(item.fileName, item.mimeType)) {
                            ApkBadgePill()
                        }
                    }
                    Text(
                        text = item.url,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pause / Resume / Retry Primary Action Button
                when (status) {
                    DownloadStatus.DOWNLOADING -> {
                        FilledTonalIconButton(
                            onClick = onPause,
                            modifier = Modifier.size(44.dp).testTag("download_pause_${item.id}")
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause download")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        FilledTonalIconButton(
                            onClick = onResume,
                            modifier = Modifier.size(44.dp).testTag("download_resume_${item.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume download")
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        FilledTonalIconButton(
                            onClick = onResume,
                            modifier = Modifier.size(44.dp).testTag("download_retry_${item.id}")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Retry download")
                        }
                    }
                    DownloadStatus.COMPLETED -> Unit
                }

                // Cancel or Delete Button
                if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(44.dp).testTag("download_cancel_${item.id}")
                    ) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Cancel download",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(44.dp).testTag("download_delete_${item.id}")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove download")
                    }
                }
            }

            // Progress Bar
            if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                if (item.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else if (status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Telemetry Footer Row (Status, Bytes, Percentage, Speed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val sizeText = if (item.totalBytes > 0L) {
                    val pct = (item.progressFraction * 100f).toInt()
                    "${formatBytes(item.bytesDownloaded)} / ${formatBytes(item.totalBytes)} ($pct%)"
                } else if (item.bytesDownloaded > 0L) {
                    formatBytes(item.bytesDownloaded)
                } else {
                    ""
                }

                val speedSuffix = if (status == DownloadStatus.DOWNLOADING && speedBytesPerSec > 0L) {
                    " • ${formatBytes(speedBytesPerSec)}/s"
                } else {
                    ""
                }

                Text(
                    text = "$sizeText$speedSuffix",
                    style = MonoCodeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompletedDownloadCard(
    item: DownloadEntity,
    formattedTime: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("completed_download_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ShieldEmerald.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = iconForMime(item.mimeType, item.fileName),
                    contentDescription = null,
                    tint = ShieldEmerald
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isApkDownload(item.fileName, item.mimeType)) {
                        ApkBadgePill()
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ShieldEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${formatBytes(item.bytesDownloaded.coerceAtLeast(item.totalBytes.coerceAtLeast(0L)))} • $formattedTime",
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onOpen, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open downloaded file", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Share downloaded file", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete downloaded file", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun ApkBadgePill() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ShieldEmerald.copy(alpha = 0.16f)
    ) {
        Text(
            text = "APP / APK",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ShieldEmerald,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

private fun iconForMime(mimeType: String, fileName: String): ImageVector {
    val mime = mimeType.lowercase()
    val name = fileName.lowercase()
    return when {
        isApkDownload(fileName, mimeType) -> Icons.Filled.Android
        mime.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp") -> Icons.Filled.Image
        mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") -> Icons.Filled.VideoFile
        mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") -> Icons.Filled.AudioFile
        mime.contains("pdf") || mime.startsWith("text/") || name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") -> Icons.Filled.Description
        mime.contains("zip") || mime.contains("rar") || mime.contains("tar") || name.endsWith(".zip") -> Icons.Filled.FolderZip
        else -> Icons.Filled.InsertDriveFile
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1.0) return "$bytes B"
    val mb = kb / 1024.0
    if (mb < 1.0) return String.format(Locale.US, "%.1f KB", kb)
    val gb = mb / 1024.0
    if (gb < 1.0) return String.format(Locale.US, "%.2f MB", mb)
    return String.format(Locale.US, "%.2f GB", gb)
}
