package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "General",
    val isSpeedDial: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val status: String = DownloadStatus.DOWNLOADING.name,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val localPath: String = "",
    val errorMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val downloadStatus: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)

    val progressFraction: Float
        get() = if (totalBytes > 0L) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

@Entity(tableName = "filter_subscriptions")
data class FilterSubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // ADS, TRACKERS, SOCIAL, ANNOYANCES, CUSTOM
    val sourceUrl: String,
    val isEnabled: Boolean = true,
    val ruleCount: Int = 0,
    val lastUpdatedAt: Long = 0L,
    val isBuiltIn: Boolean = true
)

@Entity(
    tableName = "filter_rules",
    primaryKeys = ["domain", "subscriptionId"]
)
data class FilterRuleEntity(
    val domain: String,
    val subscriptionId: String,
    val isUserCustom: Boolean = false
)

@Entity(tableName = "shield_allowlist")
data class ShieldAllowlistEntity(
    @PrimaryKey val host: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_search_engines")
data class CustomSearchEngineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val searchUrlTemplate: String
)
