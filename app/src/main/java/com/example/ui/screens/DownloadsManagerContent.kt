package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.StrokeCap
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
    ACTIVE("Active"),
    APPS("Apps (APK)"),
    MEDIA("Media"),
    DOCS("Docs"),
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

private fun isMediaDownload(fileName: String, mimeType: String): Boolean {
    val name = fileName.lowercase()
    val mime = mimeType.lowercase()
    return mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/") ||
        name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp") ||
        name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") ||
        name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac")
}

private fun isDocumentDownload(fileName: String, mimeType: String): Boolean {
    val name = fileName.lowercase()
    val mime = mimeType.lowercase()
    return mime.contains("pdf") || mime.startsWith("text/") || mime.contains("document") ||
        mime.contains("zip") || mime.contains("rar") || mime.contains("tar") ||
        name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") ||
        name.endsWith(".csv") || name.endsWith(".zip") || name.endsWith(".rar")
}

private data class FileCategoryVisual(
    val icon: ImageVector,
    val tint: Color,
    val badge: String?
)

@Composable
private fun fileVisualFor(mimeType: String, fileName: String): FileCategoryVisual {
    val mime = mimeType.lowercase()
    val name = fileName.lowercase()
    val ext = fileName.substringAfterLast('.', "").uppercase().take(5).ifBlank { null }
    return when {
        isApkDownload(fileName, mimeType) -> FileCategoryVisual(Icons.Rounded.Android, ShieldEmerald, "APK")
        mime.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp") ->
            FileCategoryVisual(Icons.Rounded.Image, Color(0xFF38BDF8), ext ?: "IMG")
        mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") ->
            FileCategoryVisual(Icons.Rounded.VideoFile, Color(0xFF60A5FA), ext ?: "VID")
        mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") ->
            FileCategoryVisual(Icons.Rounded.AudioFile, Color(0xFFA78BFA), ext ?: "AUD")
        mime.contains("pdf") || mime.startsWith("text/") || name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") ->
            FileCategoryVisual(Icons.Rounded.Description, Color(0xFFC084FC), ext ?: "DOC")
        mime.contains("zip") || mime.contains("rar") || mime.contains("tar") || name.endsWith(".zip") ->
            FileCategoryVisual(Icons.Rounded.FolderZip, WarningAmber, ext ?: "ZIP")
        else -> FileCategoryVisual(Icons.Rounded.InsertDriveFile, MaterialTheme.colorScheme.primary, ext)
    }
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
            DownloadFilter.ACTIVE -> byQuery.filter {
                it.downloadStatus == DownloadStatus.DOWNLOADING || it.downloadStatus == DownloadStatus.PAUSED
            }
            DownloadFilter.APPS -> byQuery.filter { isApkDownload(it.fileName, it.mimeType) }
            DownloadFilter.MEDIA -> byQuery.filter { isMediaDownload(it.fileName, it.mimeType) }
            DownloadFilter.DOCS -> byQuery.filter { isDocumentDownload(it.fileName, it.mimeType) }
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

    val liveDownloadingCount = remember(downloads) { downloads.count { it.downloadStatus == DownloadStatus.DOWNLOADING } }
    val pausedCount = remember(downloads) { downloads.count { it.downloadStatus == DownloadStatus.PAUSED } }
    val doneCount = remember(downloads) { downloads.count { it.downloadStatus == DownloadStatus.COMPLETED } }
    val apkCount = remember(downloads) { downloads.count { isApkDownload(it.fileName, it.mimeType) } }
    val totalSpeedBytesPerSec = remember(downloads, speedsById) {
        downloads
            .filter { it.downloadStatus == DownloadStatus.DOWNLOADING }
            .sumOf { speedsById[it.id] ?: 0L }
    }
    val totalSavedBytes = remember(downloads) {
        downloads
            .filter { it.downloadStatus == DownloadStatus.COMPLETED }
            .sumOf { it.bytesDownloaded.coerceAtLeast(it.totalBytes.coerceAtLeast(0L)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Luminous Storage & Transfer Hero Card
        DownloadsHeroCard(
            liveDownloadingCount = liveDownloadingCount,
            pausedCount = pausedCount,
            doneCount = doneCount,
            apkCount = apkCount,
            totalSpeedBytesPerSec = totalSpeedBytesPerSec,
            totalSavedBytes = totalSavedBytes,
            onAddUrlClick = { showNewDownloadDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Filter Pill Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DownloadFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val countSuffix = when (filter) {
                    DownloadFilter.ACTIVE -> (liveDownloadingCount + pausedCount).takeIf { it > 0 }?.let { " ($it)" } ?: ""
                    DownloadFilter.APPS -> apkCount.takeIf { it > 0 }?.let { " ($it)" } ?: ""
                    else -> ""
                }
                val chipIcon = when (filter) {
                    DownloadFilter.ALL -> null
                    DownloadFilter.ACTIVE -> Icons.Rounded.Bolt
                    DownloadFilter.APPS -> Icons.Rounded.Android
                    DownloadFilter.MEDIA -> Icons.Rounded.PermMedia
                    DownloadFilter.DOCS -> Icons.Rounded.Description
                    DownloadFilter.COMPLETED -> Icons.Rounded.CheckCircle
                }
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
                        .clickable { selectedFilter = filter }
                        .testTag("downloads_filter_${filter.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        if (chipIcon != null) {
                            Icon(
                                imageVector = chipIcon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "${filter.label}$countSuffix",
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

        if (filtered.isEmpty()) {
            EmptyLibraryState(
                icon = Icons.Rounded.CloudDownload,
                title = if (downloads.isEmpty()) "No Downloads Yet" else "No Matching Files",
                subtitle = if (downloads.isEmpty()) {
                    "Tap any download link while browsing, long-press media, or paste a direct file URL to begin."
                } else {
                    "Try a different filter category or clear your search query."
                },
                accentColor = ShieldEmerald,
                ctaLabel = "Download from URL",
                onCtaClick = { showNewDownloadDialog = true }
            )
        } else {
            val dateFmt = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTransfers.isNotEmpty()) {
                    item(key = "section_active") {
                        DownloadSectionHeader(
                            title = "Active & Queued Transfers",
                            count = activeTransfers.size,
                            accentColor = MaterialTheme.colorScheme.primary
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
                        DownloadSectionHeader(
                            title = "Saved Files",
                            count = completedFiles.size,
                            accentColor = ShieldEmerald,
                            modifier = Modifier.padding(top = if (activeTransfers.isNotEmpty()) 6.dp else 0.dp)
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
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (showNewDownloadDialog) {
        var urlInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDownloadDialog = false },
            title = { Text("Download from URL", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste a direct HTTPS or HTTP file link. Pause and resume are supported automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Direct File URL") },
                        placeholder = { Text("https://example.com/archive.zip") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_download_url_input")
                    )
                }
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
private fun DownloadsHeroCard(
    liveDownloadingCount: Int,
    pausedCount: Int,
    doneCount: Int,
    apkCount: Int,
    totalSpeedBytesPerSec: Long,
    totalSavedBytes: Long,
    onAddUrlClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentGlow = if (liveDownloadingCount > 0) MaterialTheme.colorScheme.primary else ShieldEmerald

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (liveDownloadingCount > 0) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            }
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentGlow.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.18f),
                            radius = size.maxDimension * 0.58f
                        )
                    )
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentGlow.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = if (liveDownloadingCount > 0) Icons.Rounded.Download else Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = accentGlow,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column {
                        Text(
                            text = when {
                                liveDownloadingCount > 0 && totalSpeedBytesPerSec > 0L ->
                                    "${formatBytes(totalSpeedBytesPerSec)}/s"
                                liveDownloadingCount > 0 ->
                                    "$liveDownloadingCount Active ${if (liveDownloadingCount == 1) "Transfer" else "Transfers"}"
                                totalSavedBytes > 0L ->
                                    formatBytes(totalSavedBytes)
                                else -> "Download Vault"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                liveDownloadingCount > 0 -> "$liveDownloadingCount downloading${if (pausedCount > 0) " • $pausedCount paused" else ""}"
                                pausedCount > 0 -> "$pausedCount paused • Tap resume to continue"
                                doneCount > 0 -> "$doneCount saved ${if (doneCount == 1) "file" else "files"} • Pause & resume ready"
                                else -> "Supports APKs, media & resumable links"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onAddUrlClick,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("downloads_add_url_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("URL", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // Compact Telemetry Stat Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadStatPill(
                    label = "Active",
                    value = (liveDownloadingCount + pausedCount).toString(),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DownloadStatPill(
                    label = "Saved",
                    value = doneCount.toString(),
                    accent = ShieldEmerald,
                    modifier = Modifier.weight(1f)
                )
                DownloadStatPill(
                    label = "Apps",
                    value = apkCount.toString(),
                    accent = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DownloadStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DownloadSectionHeader(
    title: String,
    count: Int,
    accentColor: Color,
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
                .background(accentColor)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.14f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
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
    val visual = fileVisualFor(item.mimeType, item.fileName)
    val host = remember(item.url) { extractCleanHost(item.url).ifBlank { item.url } }

    val (statusLabel, statusColor) = when (status) {
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "Paused" to WarningAmber
        DownloadStatus.FAILED -> (item.errorMessage.ifBlank { "Failed" }) to DangerCrimson
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.COMPLETED -> "Completed" to ShieldEmerald
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (status == DownloadStatus.DOWNLOADING) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_download_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconTint = if (status == DownloadStatus.FAILED) DangerCrimson else visual.tint
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconTint.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (status == DownloadStatus.FAILED) Icons.Rounded.ErrorOutline else visual.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isApkDownload(item.fileName, item.mimeType)) {
                            ApkBadgePill()
                        }
                    }
                    Text(
                        text = host,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pause / Resume / Retry Primary Control
                when (status) {
                    DownloadStatus.DOWNLOADING -> {
                        FilledTonalIconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("download_pause_${item.id}")
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause download")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        FilledTonalIconButton(
                            onClick = onResume,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("download_resume_${item.id}")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume download")
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        FilledTonalIconButton(
                            onClick = onResume,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("download_retry_${item.id}")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Retry download")
                        }
                    }
                    DownloadStatus.COMPLETED -> Unit
                }

                // Cancel or Remove Control
                if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("download_cancel_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Cancel download",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("download_delete_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rounded Progress Bar
            if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                if (item.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                } else if (status == DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Status & Speed Telemetry Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    if (status == DownloadStatus.DOWNLOADING && speedBytesPerSec > 0L) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${formatBytes(speedBytesPerSec)}/s",
                                style = MonoCodeStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                val sizeText = if (item.totalBytes > 0L) {
                    val pct = (item.progressFraction * 100f).toInt()
                    "${formatBytes(item.bytesDownloaded)} / ${formatBytes(item.totalBytes)} ($pct%)"
                } else if (item.bytesDownloaded > 0L) {
                    formatBytes(item.bytesDownloaded)
                } else {
                    ""
                }

                if (sizeText.isNotEmpty()) {
                    Text(
                        text = sizeText,
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
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
    val visual = fileVisualFor(item.mimeType, item.fileName)
    val host = remember(item.url) { extractCleanHost(item.url) }
    val sizeFormatted = remember(item.bytesDownloaded, item.totalBytes) {
        formatBytes(item.bytesDownloaded.coerceAtLeast(item.totalBytes.coerceAtLeast(0L)))
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("completed_download_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
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
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(visual.tint.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.tint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isApkDownload(item.fileName, item.mimeType)) {
                            ApkBadgePill()
                        } else if (visual.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = visual.tint.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = visual.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = visual.tint,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = ShieldEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = buildString {
                                append(sizeFormatted)
                                append(" • ")
                                append(formattedTime)
                                if (host.isNotBlank()) {
                                    append(" • ")
                                    append(host)
                                }
                            },
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Crisp Bottom Action Strip (generous 48.dp targets, never crowds the filename)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isApkDownload(item.fileName, item.mimeType)) "Install / Open" else "Open",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onShare,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share downloaded file",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete downloaded file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
            text = "APK",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = ShieldEmerald,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
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
