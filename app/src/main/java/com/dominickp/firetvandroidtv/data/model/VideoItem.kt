package com.dominickp.firetvandroidtv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VideoItem — mirrors Roku's ContentNode / video item from the feed.
 *
 * This is 1:1 with the feed structure from Roku-Server-Template:
 *   {
 *     "id": "...",
 *     "title": "...",
 *     "shortDescription": "...",
 *     "thumbnail": "...",
 *     "url": "...",           // top-level video URL (fallback when videos is empty)
 *     "releaseDate": "...",
 *     "releaseDateObj": "...",
 *     "genres": ["..."],
 *     "content": { ... },     // nested ContentNode (duration, videos[])
 *     "dateAdded": "...",
 *     "isLive": false
 *   }
 */
@Serializable
data class VideoItem(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("shortDescription") val shortDescription: String = "",
    @SerialName("thumbnail") val thumbnail: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("releaseDate") val releaseDate: String = "",
    @SerialName("releaseDateObj") val releaseDateObj: String? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
    // content is nullable — server feeds may omit it; we default to an empty VideoContent
    @SerialName("content") val content: VideoContent? = null,
    @SerialName("dateAdded") val dateAdded: String? = null,
    @SerialName("isLive") val isLive: Boolean = false,
    // Content rating for parental controls — mirrors Roku's contentRating field (#3.6)
    @SerialName("contentRating") val contentRating: String = "TV-14",
) {
    /**
     * Determine the primary video URL to play.
     * Priority:
     *   1. Top-level `url` field (for items without nested `videos[]`)
     *   2. First video from `content.videos` list
     *   3. Falls back to empty string
     */
    val playableUrl: String
        get() =
            url.ifBlank {
                content?.videos?.firstOrNull()?.url ?: ""
            }

    /**
     * Determine the video type for ExoPlayer.
     * Priority:
     *   1. First video's videoType from content.videos (falls back to "HLS")
     */
    val videoType: String
        get() = content?.videos?.firstOrNull()?.videoType ?: "HLS"

    /**
     * HD poster URL — derived from thumbnail or empty.
     */
    val hdPosterUrl: String
        get() = thumbnail

    /**
     * SD poster URL — same as thumbnail for now.
     */
    val sdPosterUrl: String
        get() = thumbnail

    /**
     * Duration in seconds (0 for live streams).
     */
    val duration: Int
        get() = content?.duration ?: 0
}

/**
 * VideoContent — mirrors Roku's ContentNode `content` field.
 */
@Serializable
data class VideoContent(
    @SerialName("duration") val duration: Int = 0,
    @SerialName("videos") val videos: List<Video> = emptyList(),
)

/**
 * Video — mirrors Roku's ContentNode video entry.
 */
@Serializable
data class Video(
    @SerialName("url") val url: String,
    @SerialName("quality") val quality: String = "HD",
    @SerialName("videoType") val videoType: String = "HLS",
    // "progressive" | "HLS" | "dash" | "MP4"
)
