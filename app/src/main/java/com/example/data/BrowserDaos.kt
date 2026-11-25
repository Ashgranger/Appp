package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY isSpeedDial DESC, createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isSpeedDial = 1 ORDER BY createdAt ASC")
    fun observeSpeedDials(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllOnce(): List<BookmarkEntity>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 500")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 12")
    suspend fun search(query: String): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt >= :sinceTimestamp")
    suspend fun deleteSince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Query("UPDATE downloads SET bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        bytesDownloaded: Long,
        totalBytes: Long,
        status: String,
        errorMessage: String = ""
    )

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String = "")

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}

@Dao
interface FilterDao {
    @Query("SELECT * FROM filter_subscriptions ORDER BY isBuiltIn DESC, name ASC")
    fun observeSubscriptions(): Flow<List<FilterSubscriptionEntity>>

    @Query("SELECT * FROM filter_subscriptions")
    suspend fun getSubscriptions(): List<FilterSubscriptionEntity>

    @Query("SELECT COUNT(*) FROM filter_subscriptions")
    suspend fun subscriptionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(subscription: FilterSubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscriptions(subscriptions: List<FilterSubscriptionEntity>)

    @Query("UPDATE filter_subscriptions SET isEnabled = :enabled WHERE id = :id")
    suspend fun setSubscriptionEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM filter_subscriptions WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomSubscription(id: String)

    @Query("SELECT domain FROM filter_rules WHERE subscriptionId IN (SELECT id FROM filter_subscriptions WHERE isEnabled = 1) OR isUserCustom = 1")
    fun observeActiveBlockedDomains(): Flow<List<String>>

    @Query("SELECT * FROM filter_rules WHERE isUserCustom = 1 ORDER BY domain ASC")
    fun observeUserCustomRules(): Flow<List<FilterRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRules(rules: List<FilterRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FilterRuleEntity)

    @Query("DELETE FROM filter_rules WHERE subscriptionId = :subscriptionId")
    suspend fun deleteRulesForSubscription(subscriptionId: String)

    @Query("DELETE FROM filter_rules WHERE domain = :domain AND isUserCustom = 1")
    suspend fun deleteUserCustomRule(domain: String)

    @Transaction
    suspend fun replaceSubscriptionRules(subscriptionId: String, domains: List<String>, updatedAt: Long) {
        deleteRulesForSubscription(subscriptionId)
        val chunks = domains.chunked(500)
        for (chunk in chunks) {
            insertRules(chunk.map { FilterRuleEntity(domain = it, subscriptionId = subscriptionId, isUserCustom = false) })
        }
        updateSubscriptionStats(subscriptionId, domains.size, updatedAt)
    }

    @Query("UPDATE filter_subscriptions SET ruleCount = :ruleCount, lastUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSubscriptionStats(id: String, ruleCount: Int, updatedAt: Long)
}

@Dao
interface ShieldAllowlistDao {
    @Query("SELECT * FROM shield_allowlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ShieldAllowlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun allowHost(entry: ShieldAllowlistEntity)

    @Query("DELETE FROM shield_allowlist WHERE host = :host")
    suspend fun removeHost(host: String)
}

@Dao
interface CustomSearchEngineDao {
    @Query("SELECT * FROM custom_search_engines ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomSearchEngineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(engine: CustomSearchEngineEntity)

    @Query("DELETE FROM custom_search_engines WHERE id = :id")
    suspend fun deleteById(id: String)
}
