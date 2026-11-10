package com.dominickp.firetvandroidtv.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * HeartbeatService — periodic server ping to keep session alive.
 *
 * Mirrors Roku's heartbeat mechanism for keeping the connection active.
 * Configured via HeartbeatConfig in app_config.json.
 */
class HeartbeatService(
    private val heartbeatUrl: String,
    private val intervalSeconds: Int = 60,
) {
    companion object {
        private const val TAG = "HeartbeatService"
        // Max wait (ms) for the off-main-thread OkHttp disposal in stop().
        // Long enough for the executor shutdown + evictAll() to complete;
        // short enough to not meaningfully delay lifecycle transitions.
        private const val CLEANUP_JOIN_TIMEOUT_MS = 2000L
    }

    // OkHttpClient. stop() cleans it up (dispatcher.executorService.shutdown()
    // + connectionPool.evictAll() — leak prevention, restored from 70912a7) and
    // sets clientShutDown. start() rebuilds a fresh client when clientShutDown is
    // true, so a background -> foreground lifecycle transition resumes the
    // heartbeat (a cleaned-up client has its dispatcher shut down and cannot
    // open new connections).
    //
    // Note: the field stays non-null after stop() so callers (and tests) that
    // reflect on it can still inspect the shut-down client; a new start() swaps
    // in a fresh instance via newClient().
    private var client: OkHttpClient = newClient()

    // Tracks whether the current client instance has been shut down by stop().
    private var clientShutDown = false

    // SupervisorJob-tied scope so the heartbeat coroutine is properly parented and
    // can be cancelled, avoiding orphaned coroutines (leak prevention).
    //
    // IMPORTANT: stop() cancels only the heartbeat child coroutine, never this
    // SupervisorJob. A cancelled Job stays cancelled, so cancelling the scope's
    // parent job would make every subsequent start() launch into a dead scope and
    // the heartbeat would silently never resume — breaking the app's
    // background -> foreground lifecycle cycles (see FireTVApplication).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running = false
    private var heartbeatJob: Job? = null

    /**
     * Start the heartbeat service.
     */
    fun start() {
        if (running) {
            Logger.w(TAG, "Heartbeat service already running")
            return
        }

        running = true
        // Rebuild the OkHttpClient if a prior stop() shut it down. A shut-down
        // client cannot open new connections, so a background -> foreground
        // transition must create a fresh one for the heartbeat to resume.
        val activeClient =
            if (clientShutDown) {
                clientShutDown = false
                newClient().also { client = it }
            } else {
                client
            }
        // Launch a fresh heartbeat coroutine on the (still-alive) scope.
        heartbeatJob =
            scope.launch {
                while (running) {
                    sendHeartbeat(activeClient)
                    delay(intervalSeconds * 1000L)
                }
            }
        Logger.i(TAG, "Heartbeat service started (interval=${intervalSeconds}s)")
    }

    /**
     * Stop the heartbeat service.
     * Cancels the running heartbeat coroutine (not the scope's parent job, which
     * would stay cancelled and break future start() calls) and cleans up the
     * OkHttpClient: shuts down the dispatcher executor and evicts pooled
     * connections, releasing threads and file descriptors (leak prevention,
     * restored from 70912a7 — the cleanup was dropped in 23f63ff's SupervisorJob
     * refactor, which is why the leak tests fail at HEAD). A later start()
     * rebuilds a fresh client (see [clientShutDown]) and resumes on the
     * still-alive scope.
     *
     * The OkHttp client disposal is offloaded to a dedicated daemon thread.
     * OkHttp's ConnectionPool.evictAll() closes pooled sockets, and Conscrypt
     * treats a socket close as a "network operation" — it consults StrictMode's
     * main-thread network guard and throws NetworkOnMainThreadException when
     * run from the UI thread. stop() is called from lifecycle callbacks
     * (onStop/onLowMemory/onTerminate) which are main-thread on device, so the
     * disposal must happen off the main thread. The join() guarantees the
     * executor is terminated before stop() returns (the resource-leak unit
     * tests assert on isTerminated immediately after stop()).
     */
    fun stop() {
        running = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        // Snapshot the client to dispose (evictAll() closes live sockets and
        // trips StrictMode's network guard when done on the main thread).
        val clientToDispose = client
        clientShutDown = true
        val cleanup =
            Thread {
                runCatching {
                    clientToDispose.dispatcher.executorService.shutdown()
                    clientToDispose.connectionPool.evictAll()
                }
            }.apply {
                isDaemon = true
                name = "HeartbeatService-cleanup"
                start()
            }
        // Wait for disposal to complete so stop() semantics are synchronous.
        try {
            cleanup.join(CLEANUP_JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            // Intentionally empty — interruption during cleanup join is safe
            // to ignore; the cleanup thread is daemon and self-contained.
        }
        Logger.i(TAG, "Heartbeat service stopped")
    }

    /**
     * Send a single heartbeat request using [activeClient] (pinned for the
     * current heartbeat run, so a concurrent stop() shutdown cannot affect it).
     */
    private fun sendHeartbeat(activeClient: OkHttpClient) {
        try {
            val request =
                Request
                    .Builder()
                    .url(heartbeatUrl)
                    .get()
                    .build()

            activeClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Logger.d(TAG, "Heartbeat successful: HTTP ${response.code}")
                } else {
                    Logger.w(TAG, "Heartbeat failed: HTTP ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, e, "Heartbeat error: ${e.message}")
        }
    }

    private fun newClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
}
