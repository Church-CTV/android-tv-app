package com.dominickp.firetvandroidtv

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.dao.ContinueWatchingDao
import com.dominickp.firetvandroidtv.data.network.FeedRepository
import com.dominickp.firetvandroidtv.data.network.createFeedApi
import com.dominickp.firetvandroidtv.db.FireTVDatabase
import com.dominickp.firetvandroidtv.util.CrashReportingService
import com.dominickp.firetvandroidtv.util.HeartbeatService
import com.dominickp.firetvandroidtv.util.Logger

/**
 * Application singleton - initializes dependency graph, global state.
 * Equivalent to Roku's MainScene + App.brs shared utilities.
 *
 * Responsibilities:
 * - Store application context reference
 * - Initialize Coil ImageLoader (disable hardware bitmaps for emulator compatibility)
 * - Initialize HeartbeatService (periodic server ping)
 * - Start/stop heartbeat on app lifecycle events using ProcessLifecycleOwner
 */
class FireTVApplication :
    Application(),
    ImageLoaderFactory {
    companion object {
        @Volatile
        var instance: FireTVApplication? = null
            @JvmStatic set

        // Coil memory cache size as percentage of app heap
        private const val COIL_MEMORY_CACHE_SIZE_PERCENT = 0.15

        /**
         * Get the application instance.
         * Throws IllegalStateException if accessed before onCreate() completes.
         */
        val application: FireTVApplication
            get() =
                instance
                    ?: throw IllegalStateException(
                        "FireTVApplication.instance not initialized. " +
                            "Ensure this is called after Application.onCreate().",
                    )
    }

    private var heartbeatService: HeartbeatService? = null
    private var isForeground = false

    /**
     * Room DB DAO — lazily initialized (first access triggers DB creation).
     * On emulator first-run this can take 2-3s due to schema creation.
     * Callers should handle null (DB not yet ready) gracefully.
     */
    var continueWatchingDao: ContinueWatchingDao? = null
        private set

    /**
     * Shared FeedRepository — single OkHttpClient across all viewmodels.
     * Initialized in onCreate(); callers treat as nullable (race-safe).
     */
    var feedRepository: FeedRepository? = null
        private set

    // Coil singleton factory - disable hardware bitmaps for API 35+ emulator compatibility
    // Limit concurrent downloads to 3 to prevent UI freeze on Fire TV
    // https://coil-kt.github.io/coil/image_loaders/
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .okHttpClient(
                okhttp3.OkHttpClient
                    .Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .connectionPool(
                        okhttp3.ConnectionPool(
                            maxIdleConnections = 3,
                            keepAliveDuration = 60,
                            java.util.concurrent.TimeUnit.SECONDS,
                        ),
                    )
                    .build(),
            )
            .crossfade(false)
            .respectCacheHeaders(true)
            .allowHardware(false)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(COIL_MEMORY_CACHE_SIZE_PERCENT)
                    .build()
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this

        val configLoader = ConfigLoader()

        try {
            val appConfig = configLoader.getConfig()

            // Shared FeedRepository (single OkHttpClient for all network ops)
            try {
                val contentCfg = appConfig.content
                val feedApi = createFeedApi(appConfig.apiBaseUrl, contentCfg)
                feedRepository = FeedRepository(feedApi, contentCfg)
            } catch (e: Exception) {
                Logger.e("FireTVApplication", e, "Failed to create FeedRepository")
            }

            // HeartbeatService (don't start yet — waits for foreground)
            try {
                val heartbeatConfig = appConfig.heartbeat
                if (heartbeatConfig.exportUrl.isNotBlank()) {
                    heartbeatService =
                        HeartbeatService(
                            heartbeatUrl = heartbeatConfig.exportUrl,
                            intervalSeconds = heartbeatConfig.intervalSeconds,
                        )
                }
            } catch (e: Exception) {
                Logger.e("FireTVApplication", e, "Failed to initialize HeartbeatService")
            }

            // CrashReportingService (#3.4)
            try {
                val analyticsConfig = appConfig.analytics
                if (analyticsConfig.exportUrl.isNotBlank()) {
                    val tracker = AnalyticsTracker.getInstance(analyticsConfig)
                    tracker.init(this) // Initialize offline persistence (#2.4)
                    CrashReportingService.getInstance().init(analyticsConfig.exportUrl, this)
                    val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
                    Thread.setDefaultUncaughtExceptionHandler { t, throwable ->
                        CrashReportingService.getInstance().reportCrash(throwable)
                        Logger.e("FireTVApplication", throwable, "Uncaught exception on thread ${t.name}")
                        existingHandler?.uncaughtException(t, throwable)
                    }
                }
            } catch (e: Exception) {
                Logger.e("FireTVApplication", e, "Failed to initialize CrashReportingService")
            }
        } catch (e: Exception) {
            Logger.e("FireTVApplication", e, "Failed to load config")
        }

        // Room database for continue-watching
        try {
            val database = FireTVDatabase.getDatabase(this)
            continueWatchingDao = database.continueWatchingDao()
        } catch (e: Exception) {
            Logger.e("FireTVApplication", e, "Failed to initialize Room database")
        }

        // Register lifecycle observer to start/stop heartbeat based on foreground state
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    if (!isForeground) {
                        isForeground = true
                        heartbeatService?.start()
                    }
                }

                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    isForeground = false
                    heartbeatService?.stop()
                }
            },
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Clear the foreground flag before stopping, exactly like onStop(). The
        // heartbeat is stopped to free threads/connections under memory pressure,
        // and the observer in onCreate() only re-starts it when `!isForeground` —
        // so the flag must be reset or the heartbeat never resumes after the trim.
        isForeground = false
        heartbeatService?.stop()
    }

    override fun onTerminate() {
        super.onTerminate()
        heartbeatService?.stop()
    }
}
