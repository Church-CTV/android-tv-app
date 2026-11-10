package com.dominickp.firetvandroidtv.config

import com.dominickp.firetvandroidtv.data.model.VideoContent
import com.dominickp.firetvandroidtv.data.model.VideoItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * RawFeedResponse — captures the raw JSON feed without pre-parsed groups.
 * The server feed uses dynamic month/category keys ("2026 07", "LIVE NOW", etc.)
 * that kotlinx.serialization needs special handling for.
 *
 * Color values from the server use Roku hex format (0xRRGGBB). These are stored
 * as raw strings and converted later by ColorUtils.decodeColor() when applied to UI.
 */
@Serializable
data class RawFeedResponse(
    @SerialName("providerName") val providerName: String = "",
    @SerialName("lastUpdated") val lastUpdated: String = "",
    @SerialName("language") val language: String = "en",
    val cardStyle: CardStyleConfig? = null,
    val config: AppConfig? = null,
) {
    companion object {
        private val knownKeys =
            setOf(
                "providerName",
                "lastUpdated",
                "contentFeedVersion",
                "testFeed",
                "language",
                "cardStyle",
                "config",
            )

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        /**
         * Parse a raw JSON feed string, extracting dynamic month/category groups.
         * Returns (metadata, groupsMap) where groupsMap maps "January 2026" → [VideoItem, ...].
         *
         * Returns Result — failure if the JSON is completely unparseable.
         */
        fun parse(jsonString: String): Result<Pair<RawFeedResponse, Map<String, List<VideoItem>>>> {
            return try {
                val root = json.parseToJsonElement(jsonString)
                if (root !is JsonObject) {
                    return Result.failure(IllegalArgumentException("Feed root is not a JSON object"))
                }
                Result.success(parseJsonObject(root))
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

        private fun parseJsonObject(obj: JsonObject): Pair<RawFeedResponse, Map<String, List<VideoItem>>> {
            // Extract metadata fields
            val metadata =
                RawFeedResponse(
                    providerName = obj["providerName"]?.jsonPrimitive?.content ?: "",
                    lastUpdated = obj["lastUpdated"]?.jsonPrimitive?.content ?: "",
                    language = obj["language"]?.jsonPrimitive?.content ?: "en",
                    cardStyle = obj["cardStyle"]?.let { json.decodeFromJsonElement(CardStyleConfig.serializer(), it) },
                    config = obj["config"]?.let { json.decodeFromJsonElement(AppConfig.serializer(), it) },
                )

            // Extract dynamic groups
            val groups = mutableMapOf<String, List<VideoItem>>()
            for ((key, value) in obj) {
                if (key !in knownKeys && value is JsonArray) {
                    val videos = parseVideoArray(value)
                    groups[key] = videos
                }
            }

            return metadata to groups
        }

        private fun parseVideoArray(array: JsonArray): List<VideoItem> {
            return array.map { element ->
                try {
                    json.decodeFromJsonElement(VideoItem.serializer(), element)
                } catch (e: kotlinx.serialization.SerializationException) {
                    // Skip malformed video entries — don't crash the entire feed
                    VideoItem(
                        id = "parse_error_${System.currentTimeMillis()}",
                        title = "Parse Error",
                        content = VideoContent(),
                    )
                }
            }
        }

        /**
         * Parse search results from the server.
         * The search endpoint returns a JSON array of video items (same structure as feed groups).
         * Returns a list of VideoItem objects.
         */
        fun parseSearch(jsonString: String): List<VideoItem> {
            return try {
                val root = json.parseToJsonElement(jsonString)
                if (root is JsonArray) {
                    root.map { element ->
                        json.decodeFromJsonElement(VideoItem.serializer(), element)
                    }
                } else {
                    // If it's an object with a "results" or "videos" key, extract the array
                    val obj = root.jsonObject
                    val array = obj["results"] ?: obj["videos"] ?: root
                    if (array is JsonArray) {
                        array.map { element ->
                            json.decodeFromJsonElement(VideoItem.serializer(), element)
                        }
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, return empty list — search is non-critical
                emptyList()
            }
        }
    }
}
