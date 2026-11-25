package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        FilterSubscriptionEntity::class,
        FilterRuleEntity::class,
        ShieldAllowlistEntity::class,
        CustomSearchEngineEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SwiftWebDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun filterDao(): FilterDao
    abstract fun shieldAllowlistDao(): ShieldAllowlistDao
    abstract fun customSearchEngineDao(): CustomSearchEngineDao

    companion object {
        @Volatile
        private var INSTANCE: SwiftWebDatabase? = null

        fun getInstance(context: Context): SwiftWebDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SwiftWebDatabase::class.java,
                    "swiftweb_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
