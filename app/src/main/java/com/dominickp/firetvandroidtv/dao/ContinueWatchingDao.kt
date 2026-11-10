package com.dominickp.firetvandroidtv.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * ContinueWatching — Room entity for tracking watched videos.
 *
 * Mirrors Roku's ContinueWatching feature which tracks:
 *   - videoId (unique identifier)
 *   - title (for display in the ContinueWatching row)
 *   - thumbnail (for the card image)
 *   - playedPercentage (how far through the video the user watched, 0-100)
 *   - lastWatched (timestamp of last play session)
 *
 * Data is stored locally in Room DB and shown as a special "Continue Watching"
 * row at the top of the LeftNavScreen content area.
 */
@Entity
@kotlinx.serialization.Serializable
data class ContinueWatchingEntry(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnail: String = "",
    val playedPercentage: Int = 0,
    val lastWatched: Long = 0L,
    val contentUrl: String = "",
)

/**
 * ContinueWatchingDao — Room DAO for ContinueWatching operations.
 *
 * Supports:
 *   - insertOrUpdate: upsert a video entry (replaces existing or inserts new)
 *   - delete: remove a video from continue watching
 *   - getAll: get all entries sorted by lastWatched (most recent first)
 *   - getActive: get entries with playedPercentage < 100 (not fully watched)
 *   - markAsWatched: set playedPercentage to 100 (remove from ContinueWatching row)
 */
@Dao
interface ContinueWatchingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: ContinueWatchingEntry)

    @Delete
    suspend fun delete(entry: ContinueWatchingEntry)

    @Query("SELECT * FROM ContinueWatchingEntry ORDER BY lastWatched DESC")
    fun getAll(): Flow<List<ContinueWatchingEntry>>

    @Query("SELECT * FROM ContinueWatchingEntry WHERE playedPercentage < 100 ORDER BY lastWatched DESC")
    fun getActive(): Flow<List<ContinueWatchingEntry>>

    @Query("UPDATE ContinueWatchingEntry SET playedPercentage = 100, lastWatched = :timestamp WHERE videoId = :videoId")
    suspend fun markAsWatched(
        videoId: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("SELECT COUNT(*) FROM ContinueWatchingEntry WHERE playedPercentage < 100")
    fun getActiveCount(): Flow<Int>

    @Query("DELETE FROM ContinueWatchingEntry WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)
}
