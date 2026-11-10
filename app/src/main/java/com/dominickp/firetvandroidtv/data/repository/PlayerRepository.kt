package com.dominickp.firetvandroidtv.data.repository

import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.dao.ContinueWatchingEntry
import kotlinx.coroutines.flow.Flow

/**
 * PlayerRepository — manages video playback and continue-watching data.
 *
 * Mirrors Roku's PlayerRepository / PlayerState behavior:
 *   - Saves playback position during playback (every 10 seconds)
 *   - Loads continue-watching data from Room DB
 *   - Marks videos as watched
 *
 * This is the data layer for the PlayerViewModel.
 */
class PlayerRepository(
    private val continueWatchingDao: ContinueWatchingDao,
) {
    /**
     * Get all continue-watching entries (not fully watched).
     */
    fun getContinueWatching(): Flow<List<ContinueWatchingEntry>> {
        return continueWatchingDao.getActive()
    }

    /**
     * Save playback position for a video.
     * Called every 10 seconds during playback.
     *
     * Mirrors Roku's heartbeat behavior for continue watching.
     */
    suspend fun savePlaybackPosition(
        videoId: String,
        title: String,
        thumbnail: String,
        playedPercentage: Int,
        contentUrl: String,
    ) {
        val entry =
            ContinueWatchingEntry(
                videoId = videoId,
                title = title,
                thumbnail = thumbnail,
                playedPercentage = playedPercentage,
                contentUrl = contentUrl,
            )
        continueWatchingDao.insertOrUpdate(entry)
    }

    /**
     * Mark a video as fully watched (remove from continue watching).
     */
    suspend fun markAsWatched(videoId: String) {
        continueWatchingDao.markAsWatched(videoId)
    }

    /**
     * Delete a video from continue watching (user dismissed it).
     */
    suspend fun removeFromContinueWatching(videoId: String) {
        continueWatchingDao.deleteById(videoId)
    }
}
