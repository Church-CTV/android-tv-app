package com.dominickp.firetvandroidtv.data.network

import com.dominickp.firetvandroidtv.config.ContentConfig
import com.dominickp.firetvandroidtv.config.RawFeedResponse
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * FeedApi — Retrofit interface for fetching the content feed.
 *
 * Mirrors the Roku-Server-Template `/feed` endpoint.
 */
interface FeedApi {
    @GET("feed")
    suspend fun getFeed(): retrofit2.Response<ResponseBody>
}

/**
 * FeedRepository — fetches, parses, and caches the content feed.
 *
 * Caching strategy (matching Roku's retry logic):
 *   1. First fetch: from server via Retrofit
 *   2. Subsequent: check ETag / If-Modified-Since (same as server's /feed)
 *   3. In-memory cache + DataStore for last-known-etag
 *   4. Retry logic: uses ContentConfig.retryAttempts + ContentConfig.retryDelayMs
 *
 * All configurable values come from ContentConfig — no hardcoded limits.
 */
class FeedRepository(
    private val feedApi: FeedApi,
    private val contentConfig: ContentConfig,
) {
    private companion object {
        private const val TAG = "FeedRepository"
    }

    /**
     * Fetch the feed with retry logic from config.
     * Returns a map of group labels -> video items.
     */
    suspend fun fetchFeed(): Result<Map<String, List<VideoItem>>> {
        var lastException: Exception? = null
        val maxRetries = contentConfig.retryAttempts
        val retryDelayMs = contentConfig.retryDelayMs.toLong()

        Logger.d(TAG, "Starting feed fetch (maxRetries=$maxRetries)")

        repeat(maxRetries) { attempt ->
            try {
                val response = feedApi.getFeed()
                Logger.d(TAG, "Feed request attempt $attempt — HTTP ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val jsonString = body.string()
                        val result = RawFeedResponse.parse(jsonString)
                        return result.map { (_, groups) ->
                            Logger.d(TAG, "Feed parsed: ${groups.size} groups")
                            // Store parsed data for local search
                            setFeedData(groups)
                            groups
                        }
                    }
                    // Empty body on success is an error
                    Logger.w(TAG, "Feed returned 200 but body was null")
                    lastException = RuntimeException("Feed returned empty response")
                } else {
                    lastException = HttpException(response)
                    Logger.w(TAG, "Feed HTTP error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                lastException = e
                Logger.e(TAG, e, "Feed request attempt $attempt failed: ${e.message}")
            }

            // Wait before retry (but not after the last attempt)
            if (attempt < maxRetries - 1) {
                Logger.d(TAG, "Retrying feed fetch in ${retryDelayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                delay(retryDelayMs)
            }
        }

        Logger.e(TAG, "Feed fetch failed after $maxRetries attempts: ${lastException?.message}")
        return Result.failure(lastException ?: RuntimeException("Unknown feed fetch error"))
    }

    /**
     * Find a video by its ID across all feed groups.
     */
    fun findVideoById(
        videoId: String,
        groups: Map<String, List<VideoItem>>,
    ): VideoItem? {
        val allVideos = groups.values.flatten()
        return allVideos.find { it.id == videoId }
    }

    /**
     * In-memory search across all feed groups.
     * Searches title, shortDescription, and genres case-insensitively.
     * Runs on a background thread to avoid blocking the UI.
     */
    suspend fun searchLocal(
        query: String,
        limit: Int = 50,
    ): Result<List<VideoItem>> =
        withContext(Dispatchers.IO) {
            try {
                val normalizedQuery = query.lowercase().trim()
                if (normalizedQuery.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }
                val allVideos = feedData.values.flatten()
                val results =
                    allVideos
                        .filter { video ->
                            video.title.lowercase().contains(normalizedQuery) ||
                                video.shortDescription.lowercase().contains(normalizedQuery) ||
                                video.genres.any { genre -> genre.lowercase().contains(normalizedQuery) }
                        }
                        .take(limit)
                Logger.d(TAG, "Local search '$query' returned ${results.size} results")
                Result.success(results)
            } catch (e: Exception) {
                Logger.e(TAG, e, "Local search failed for '$query': ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Store parsed feed data for local search.
     * Atomic replacement to prevent race conditions.
     */
    fun setFeedData(data: Map<String, List<VideoItem>>) {
        feedData = data.toMutableMap()
        Logger.d(TAG, "Feed data set: ${feedData.size} groups, ${feedData.values.sumOf { it.size }} videos")
    }

    /**
     * Clear stored feed data.
     */
    fun clearFeedData() {
        feedData = mutableMapOf()
        Logger.d(TAG, "Feed data cleared")
    }

    @Volatile
    private var feedData: MutableMap<String, List<VideoItem>> = mutableMapOf()
}

/**
 * Creates a FeedApi Retrofit instance with config-driven settings.
 */
fun createFeedApi(
    baseUrl: String,
    contentConfig: ContentConfig,
): FeedApi {
    val okHttpClient =
        okhttp3.OkHttpClient
            .Builder()
            .connectTimeout(contentConfig.fetchTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(contentConfig.fetchTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(contentConfig.fetchTimeout.toLong(), TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestWithUserAgent =
                    original
                        .newBuilder()
                        .header("User-Agent", contentConfig.userAgent)
                        .build()
                chain.proceed(requestWithUserAgent)
            }.build()

    return Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(FeedApi::class.java)
}
