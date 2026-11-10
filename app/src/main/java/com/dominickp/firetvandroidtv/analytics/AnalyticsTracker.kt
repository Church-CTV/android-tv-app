package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Analytics events — mirrors Roku's heartbeat / analytics tracking.
 *
 * Event types:
 *   - impression: card shown on screen
 *   - play: user started playback
 *   - pause: user paused playback
 *   - complete: video finished playing
 *   - search: user performed a search
 *   - subscribe: user initiated subscription flow
 *   - error: playback or network error
 */
@Serializable(with = AnalyticsEventSerializer::class)
sealed interface AnalyticsEvent {
    @Serializable
    data class Impression(
        val videoId: String,
        val rowLabel: String,
        val position: Int,
        val cardType: String = "contentCard",
    ) : AnalyticsEvent

    @Serializable
    data class Play(
        val videoId: String,
        val streamUrl: String,
        val isLive: Boolean,
        val referrer: String = "home",
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent

    @Serializable
    data class Pause(
        val videoId: String,
        val progress: Float,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent

    @Serializable
    data class Complete(
        val videoId: String,
        val progress: Float,
        val duration: Long,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent

    @Serializable
    data class Search(
        val query: String,
        val resultCount: Int,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent

    @Serializable
    data class Subscribe(
        val plan: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent

    @Serializable
    data class Error(
        val errorType: String,
        val message: String,
        val context: String = "",
        val timestamp: Long = System.currentTimeMillis(),
    ) : AnalyticsEvent
}

/**
 * Custom serializer for AnalyticsEvent that uses a flat "type" discriminator.
 *
 * Serializes to: {"type":"impression","videoId":"..."}
 * Deserializes from: {"type":"impression","videoId":"..."}
 *
 * This matches the test expectations and provides stable JSON format across versions.
 */
object AnalyticsEventSerializer : KSerializer<AnalyticsEvent> {
    private const val TYPE_KEY = "type"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AnalyticsEvent") {
            element<String>(TYPE_KEY)
        }

    override fun serialize(
        encoder: Encoder,
        value: AnalyticsEvent,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("AnalyticsEventSerializer requires JsonEncoder")

        val type =
            when (value) {
                is AnalyticsEvent.Impression -> "impression"
                is AnalyticsEvent.Play -> "play"
                is AnalyticsEvent.Pause -> "pause"
                is AnalyticsEvent.Complete -> "complete"
                is AnalyticsEvent.Search -> "search"
                is AnalyticsEvent.Subscribe -> "subscribe"
                is AnalyticsEvent.Error -> "error"
            }

        val json =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            }

        // Use the concrete subtype's serializer directly to avoid recursion through
        // the AnalyticsEvent serializer. Each subtype is @Serializable with its own
        // serializer that doesn't invoke AnalyticsEventSerializer.
        val typeJson =
            when (value) {
                is AnalyticsEvent.Impression -> json.encodeToJsonElement(AnalyticsEvent.Impression.serializer(), value)
                is AnalyticsEvent.Play -> json.encodeToJsonElement(AnalyticsEvent.Play.serializer(), value)
                is AnalyticsEvent.Pause -> json.encodeToJsonElement(AnalyticsEvent.Pause.serializer(), value)
                is AnalyticsEvent.Complete -> json.encodeToJsonElement(AnalyticsEvent.Complete.serializer(), value)
                is AnalyticsEvent.Search -> json.encodeToJsonElement(AnalyticsEvent.Search.serializer(), value)
                is AnalyticsEvent.Subscribe -> json.encodeToJsonElement(AnalyticsEvent.Subscribe.serializer(), value)
                is AnalyticsEvent.Error -> json.encodeToJsonElement(AnalyticsEvent.Error.serializer(), value)
            }
        val merged =
            typeJson as? JsonObject
                ?: throw SerializationException("Unexpected non-object JSON")

        val result =
            JsonObject(
                merged.toMutableMap().apply {
                    put(TYPE_KEY, JsonPrimitive(type))
                },
            )

        jsonEncoder.encodeJsonElement(result)
    }

    override fun deserialize(decoder: Decoder): AnalyticsEvent {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("AnalyticsEventSerializer requires JsonDecoder")

        val jsonElement = jsonDecoder.decodeJsonElement()
        val obj =
            jsonElement as? JsonObject
                ?: throw SerializationException("Expected JSON object for AnalyticsEvent")

        val type = obj[TYPE_KEY]?.toString()?.removeSurrounding("\"")

        val json =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            }

        return when (type) {
            "impression" -> json.decodeFromJsonElement(AnalyticsEvent.Impression.serializer(), jsonElement)
            "play" -> json.decodeFromJsonElement(AnalyticsEvent.Play.serializer(), jsonElement)
            "pause" -> json.decodeFromJsonElement(AnalyticsEvent.Pause.serializer(), jsonElement)
            "complete" -> json.decodeFromJsonElement(AnalyticsEvent.Complete.serializer(), jsonElement)
            "search" -> json.decodeFromJsonElement(AnalyticsEvent.Search.serializer(), jsonElement)
            "subscribe" -> json.decodeFromJsonElement(AnalyticsEvent.Subscribe.serializer(), jsonElement)
            "error" -> json.decodeFromJsonElement(AnalyticsEvent.Error.serializer(), jsonElement)
            null -> AnalyticsEvent.Error("unknown", "Missing type", "")
            else -> AnalyticsEvent.Error("unknown", "Unknown event type", "")
        }
    }
}

/**
 * Serialization helpers for AnalyticsEvent — moved to companion object
 * to reduce AnalyticsTracker class function count (detekt TooManyFunctions).
 *
 * Uses kotlinx.serialization directly — AnalyticsEvent is @Serializable
 * with a custom serializer (AnalyticsEventSerializer) that provides flat
 * "type" discriminators like {"type":"impression",...}.
 */
@Suppress("unused") // called from AnalyticsTracker
object AnalyticsEventListHelper {
    private const val TAG = "AnalyticsTracker"

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun serializeEventList(events: List<AnalyticsEvent>): String {
        return json.encodeToString(ListSerializer(AnalyticsEvent.serializer()), events)
    }

    fun deserializeEventList(jsonStr: String): List<AnalyticsEvent> {
        return try {
            json.decodeFromString(ListSerializer(AnalyticsEvent.serializer()), jsonStr)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to deserialize event list: ${e.message}")
            emptyList()
        }
    }
}

/**
 * AnalyticsTracker — singleton for tracking analytics events.
 *
 * - Collects events in memory (bounded queue)
 * - Batches and exports to analytics endpoint via POST
 * - Uses a SHARED OkHttpClient for connection pooling
 * - Scheduled flush at config-driven interval
 * - Graceful degradation when analytics endpoint is down
 * - Offline queue persistence — survives app crash via disk-backed cache file
 * - Event deduplication — prevents duplicate impression events within a sliding window
 */
class AnalyticsTracker private constructor(
    private val analyticsConfig: com.dominickp.firetvandroidtv.config.AnalyticsConfig,
) {
    companion object {
        private const val TAG = "AnalyticsTracker"

        @Suppress("StaticFieldLeak") // singleton pattern — instance holds Application context, not Activity
        private var instance: AnalyticsTracker? = null

        @Volatile
        private var sharedClient: OkHttpClient? = null

        @Volatile
        private var sharedExecutor: java.util.concurrent.ExecutorService? = null

        private fun sharedHttpClient(): OkHttpClient {
            return sharedClient ?: synchronized(this) {
                sharedClient ?: OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build().also { sharedClient = it }
            }
        }

        private fun sharedExportExecutor(): java.util.concurrent.ExecutorService {
            return sharedExecutor ?: synchronized(this) {
                sharedExecutor ?: Executors.newSingleThreadExecutor { r ->
                    Thread(r, "AnalyticsExport").apply { isDaemon = true }
                }.also { sharedExecutor = it }
            }
        }

        /**
         * Get the singleton instance (no-arg).
         * Safe to call after FireTVApplication.onCreate() has initialized it.
         * Uses a default config as a fallback — config is only used on first creation.
         */
        fun getInstance(): AnalyticsTracker {
            return getInstance(com.dominickp.firetvandroidtv.config.AnalyticsConfig())
        }

        /**
         * Get the singleton instance with explicit config.
         * Config is only used on first creation; subsequent calls return the existing instance.
         */
        fun getInstance(analyticsConfig: com.dominickp.firetvandroidtv.config.AnalyticsConfig): AnalyticsTracker {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsTracker(analyticsConfig).also {
                    instance = it
                    it.startScheduledFlush()
                }
            }
        }

        fun resetInstance() {
            synchronized(this) {
                instance?.stopScheduledFlush()
                instance = null
                sharedExecutor?.shutdown()
                sharedExecutor = null
                // Close the shared OkHttpClient to release connection pool threads
                sharedClient?.connectionPool?.evictAll()
                sharedClient?.dispatcher?.executorService?.shutdown()
                sharedClient = null
            }
        }
    }

    private val eventQueue = mutableListOf<AnalyticsEvent>()
    private val maxQueueSize = 100
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }

    // Offline queue persistence — disk-backed cache for crash recovery.
    // Suppressed: context is always set to Application.context (not Activity), so no leak.
    @Suppress("StaticFieldLeak")
    private var context: android.content.Context? = null

    // Event deduplication — sliding window tracking impression signatures to prevent duplicates.
    // Each entry: signature string → timestamp. Old entries are pruned by cleanupOldSignatures().
    // Synchronized via eventQueue lock to prevent data races with track() and cleanupOldSignatures().
    private val recentImpressionSignatures = mutableMapOf<String, Long>()
    private val dedupWindowMs = 5 * 60 * 1000L // 5 minutes

    // Scheduled executor for periodic flush and signature cleanup — resettable for instance recreation
    private var flushScheduler: java.util.concurrent.ScheduledExecutorService? =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "AnalyticsFlush").apply { isDaemon = true }
        }
    private var flushJob: java.util.concurrent.ScheduledFuture<*>? = null

    init {
        startSignatureCleanup()
    }

    // =========================================================================
    // Offline Queue Persistence (#2.4)
    // =========================================================================

    /**
     * Initialize offline persistence with application context.
     * Called once from FireTVApplication.onCreate().
     */
    fun init(applicationContext: android.content.Context) {
        this.context = applicationContext.applicationContext
        val persisted = loadPersistedQueue()
        if (persisted.isNotEmpty()) {
            synchronized(eventQueue) {
                eventQueue.addAll(persisted)
                Logger.d(TAG, "Loaded ${persisted.size} persisted events from disk")
            }
        }
    }

    private fun persistQueue() {
        val ctx = context ?: return
        try {
            val queueFile = java.io.File(ctx.cacheDir, "analytics_queue.json")
            val snapshot = synchronized(eventQueue) { eventQueue.toList() }
            if (snapshot.isEmpty()) {
                queueFile.delete()
                return
            }
            queueFile.writeText(AnalyticsEventListHelper.serializeEventList(snapshot))
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to persist event queue: ${e.message}")
        }
    }

    private fun loadPersistedQueue(): List<AnalyticsEvent> {
        val ctx = context ?: return emptyList()
        return try {
            val queueFile = java.io.File(ctx.cacheDir, "analytics_queue.json")
            if (!queueFile.exists()) {
                emptyList()
            } else {
                AnalyticsEventListHelper.deserializeEventList(queueFile.readText())
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to load persisted queue: ${e.message}")
            emptyList()
        }
    }

    // =========================================================================
    // Event Deduplication (#3.8)
    // =========================================================================

    private fun getImpressionSignature(event: AnalyticsEvent.Impression): String {
        return "${event.videoId}:${event.rowLabel}:${event.position}"
    }

    /**
     * Track an analytics event with deduplication for impressions and persistence after export.
     */
    fun track(event: AnalyticsEvent) {
        var shouldExport = false
        synchronized(eventQueue) {
            // Deduplicate impressions within the sliding window — checked under lock
            // to prevent TOCTOU races when multiple threads call track() concurrently.
            if (event is AnalyticsEvent.Impression) {
                val signature = getImpressionSignature(event)
                val now = System.currentTimeMillis()
                val lastSeen = recentImpressionSignatures[signature]
                if (lastSeen != null && (now - lastSeen) < dedupWindowMs) return
            }

            if (eventQueue.size >= maxQueueSize) {
                eventQueue.removeAt(0)
                Logger.w(TAG, "Event queue full, dropping oldest event")
            }
            eventQueue.add(event)

            // Track impression signature with timestamp for deduplication
            if (event is AnalyticsEvent.Impression) {
                recentImpressionSignatures[getImpressionSignature(event)] = System.currentTimeMillis()
            }

            // Flag for export outside synchronized block to avoid potential deadlock
            if (eventQueue.size >= maxQueueSize / 2) {
                shouldExport = true
            }
        }
        // Call exportBatch() outside synchronized block to prevent deadlock
        if (shouldExport) {
            exportBatch()
        }
    }

    fun trackImpression(
        videoId: String,
        rowLabel: String,
        position: Int,
    ) {
        track(AnalyticsEvent.Impression(videoId, rowLabel, position))
    }

    fun trackPlay(
        videoId: String,
        streamUrl: String,
        isLive: Boolean,
        referrer: String,
    ) {
        track(AnalyticsEvent.Play(videoId, streamUrl, isLive, referrer))
    }

    fun trackPause(
        videoId: String,
        progress: Float,
    ) {
        track(AnalyticsEvent.Pause(videoId, progress))
    }

    fun trackComplete(
        videoId: String,
        progress: Float,
        duration: Long,
    ) {
        track(AnalyticsEvent.Complete(videoId, progress, duration))
    }

    fun trackSearch(
        query: String,
        resultCount: Int,
    ) {
        track(AnalyticsEvent.Search(query, resultCount))
    }

    fun trackSubscribe(plan: String) {
        track(AnalyticsEvent.Subscribe(plan))
    }

    fun trackError(
        errorType: String,
        message: String,
        context: String = "",
    ) {
        track(AnalyticsEvent.Error(errorType, message, context))
    }

    // =========================================================================
    // Signature Cleanup (#3.8)
    // =========================================================================

    private fun startSignatureCleanup() {
        flushScheduler?.scheduleWithFixedDelay(
            { cleanupOldSignatures() },
            5 * 60,
            5 * 60,
            TimeUnit.SECONDS,
        )
    }

    /**
     * Periodic cleanup of old impression signatures (every 5 minutes).
     * Only removes entries older than the dedup window; keeps recent ones.
     * Synchronized with eventQueue to prevent data races with track().
     */
    private fun cleanupOldSignatures() {
        synchronized(eventQueue) {
            val cutoff = System.currentTimeMillis() - dedupWindowMs
            recentImpressionSignatures.entries.removeAll { it.value < cutoff }
        }
    }

    // =========================================================================
    // Export / Flush
    // =========================================================================

    /**
     * Export a batch of events with persistence support.
     * On success: persists remaining queue to disk.
     * On failure: restores events to queue + persists.
     * Called from track() (main thread) or scheduled executor — always delegates to background.
     * @param retryDepth How many times this has been re-flushed after a failure (bounded by 1).
     */
    private fun exportBatch(retryDepth: Int = 0) {
        val result =
            synchronized(eventQueue) {
                if (eventQueue.isEmpty()) return
                val exportUrl = analyticsConfig.exportUrl
                if (exportUrl.isBlank()) {
                    eventQueue.forEach { event ->
                        val jsonElement = json.encodeToJsonElement(event)
                        Logger.d(TAG, "Event (no export URL): $jsonElement")
                    }
                    Logger.d(TAG, "Cleared ${eventQueue.size} events (no export URL)")
                    eventQueue.clear()
                    null
                } else if (!analyticsConfig.enabled) {
                    eventQueue.clear()
                    null
                } else {
                    val batch = eventQueue.toList()
                    eventQueue.clear()
                    batch to exportUrl
                }
            }
        result ?: return
        // Always run on background thread to avoid NetworkOnMainThreadException
        sendBatchAsync(result.first, result.second, analyticsConfig.flushOnError, retryDepth)
    }

    /**
     * Send a batch of events via HTTP POST on a background thread.
     * When flushOnError=true, re-flushes all events immediately after a failure (bounded to 1 retry).
     */
    private fun sendBatchAsync(
        batch: List<AnalyticsEvent>,
        exportUrl: String,
        flushOnError: Boolean,
        retryDepth: Int,
    ) {
        val requestBody = AnalyticsEventListHelper.serializeEventList(batch)
        // Use shared executor to avoid creating a new thread pool per export
        sharedExportExecutor().execute {
            try {
                val request =
                    Request.Builder()
                        .url(exportUrl)
                        .post(
                            requestBody.toRequestBody("application/json".toMediaType()),
                        )
                        .header("Content-Type", "application/json")
                        .build()

                sharedHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Logger.d(TAG, "Exported ${batch.size} events to $exportUrl")
                        // Persist remaining queue after successful export (#2.4)
                        persistQueue()
                    } else {
                        Logger.w(TAG, "Export failed: HTTP ${response.code}")
                        handleExportFailure(batch, flushOnError, retryDepth)
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, e, "Export error: ${e.message}")
                handleExportFailure(batch, flushOnError, retryDepth)
            }
        }
    }

    /**
     * Handle export failure: restore events to queue, persist, and optionally re-flush.
     * Retries are bounded by 1 to prevent infinite loops when the server stays down.
     */
    private fun handleExportFailure(
        batch: List<AnalyticsEvent>,
        flushOnError: Boolean,
        retryDepth: Int,
    ) {
        synchronized(eventQueue) {
            eventQueue.addAll(batch)
        }
        persistQueue()
        if (flushOnError && retryDepth < 1) {
            exportBatch(retryDepth + 1)
        }
    }

    /**
     * Force export all queued events.
     */
    fun flush() {
        exportBatch()
    }

    /**
     * Start scheduled flush at config-driven interval.
     */
    private fun startScheduledFlush() {
        val intervalSeconds = analyticsConfig.exportIntervalSeconds.toLong()
        if (intervalSeconds <= 0) return

        flushScheduler?.let { scheduler ->
            flushJob =
                scheduler.scheduleWithFixedDelay(
                    { exportBatch() },
                    intervalSeconds,
                    intervalSeconds,
                    TimeUnit.SECONDS,
                )
        }
    }

    /**
     * Stop the scheduled flush executor.
     */
    private fun stopScheduledFlush() {
        flushJob?.cancel(false)
        flushJob = null
        flushScheduler?.shutdown()
        flushScheduler = null
    }

    /**
     * Shutdown the tracker completely (for tests).
     */
    fun shutdown() {
        stopScheduledFlush()
        flush()
    }

    // ── Test accessors ──────────────────────────────────────────────

    /**
     * Get the current queue size (for testing).
     */
    internal fun queueSize(): Int = synchronized(eventQueue) { eventQueue.size }

    /**
     * Trigger signature cleanup (for testing).
     * Exposes the private cleanupOldSignatures() method.
     */
    internal fun cleanupSignatures() {
        cleanupOldSignatures()
    }
}
