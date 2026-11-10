package com.dominickp.firetvandroidtv.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry

/**
 * FireTVDatabase — Room database singleton for the app.
 *
 * Currently contains only the ContinueWatching entity.
 * Future additions: SearchHistory, WatchList, etc.
 *
 * Mirrors Roku's local DB (SG object for continueWatching state).
 */
@Database(
    entities = [ContinueWatchingEntry::class],
    version = 1,
    exportSchema = true,
)
abstract class FireTVDatabase : RoomDatabase() {
    abstract fun continueWatchingDao(): ContinueWatchingDao

    companion object {
        @Volatile
        private var INSTANCE: FireTVDatabase? = null

        fun getDatabase(context: Context): FireTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        FireTVDatabase::class.java,
                        "firetv_database",
                    )
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
