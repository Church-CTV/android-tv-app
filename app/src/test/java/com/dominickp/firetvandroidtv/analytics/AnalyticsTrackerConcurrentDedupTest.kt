package com.dominickp.firetvandroidtv.analytics

import com.dominickp.firetvandroidtv.config.AnalyticsConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * AnalyticsTrackerConcurrentDedupTest — verifies that impression deduplication
 * is thread-safe when called concurrently from multiple threads.
 *
 * Bug: recentImpressionSignatures is a plain mutableMapOf accessed from
 * the main thread (track()) and the scheduled cleanup thread (cleanupOldSignatures())
 * without synchronization — a HashMap data race that can cause
 * ConcurrentModificationException or lost updates.
 *
 * Tests concurrent impression tracking and concurrent cleanup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalyticsTrackerConcurrentDedupTest {
    private val config =
        AnalyticsConfig(
            enabled = false,
            exportUrl = "",
            exportIntervalSeconds = 0,
        )

    @After
    fun tearDown() {
        AnalyticsTracker.resetInstance()
    }

    /**
     * Concurrent impressions with identical signature must deduplicate correctly.
     *
     * 10 threads each track the same impression 100 times concurrently.
     * Only 1 event should end up in the queue.
     */
    @Test
    fun `concurrentIdenticalImpressionsAreDeduplicated()`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)
            val numThreads = 10
            val impressionsPerThread = 100
            val latch = CountDownLatch(numThreads)
            val errors = AtomicInteger(0)

            val executor = Executors.newFixedThreadPool(numThreads)
            repeat(numThreads) {
                executor.submit {
                    try {
                        repeat(impressionsPerThread) {
                            tracker.trackImpression("video1", "Home Row", 0)
                        }
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertEquals(0, errors.get())
            val queueSize = tracker.queueSize()
            assertEquals(
                "Only 1 impression should be in queue (dedup), got $queueSize — " +
                    "concurrent dedup is broken",
                1,
                queueSize,
            )
        }

    /**
     * Concurrent impressions with different signatures must all be kept.
     *
     * 10 threads each track a unique impression 100 times concurrently.
     * All 10 unique impressions should be in the queue.
     */
    @Test
    fun `concurrentDifferentImpressionsAreAllKept()`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)
            val numThreads = 10
            val impressionsPerThread = 100
            val latch = CountDownLatch(numThreads)
            val errors = AtomicInteger(0)

            val executor = Executors.newFixedThreadPool(numThreads)
            repeat(numThreads) { threadId ->
                executor.submit {
                    try {
                        repeat(impressionsPerThread) {
                            tracker.trackImpression("video_$threadId", "Home Row", 0)
                        }
                    } catch (e: Exception) {
                        errors.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertEquals(0, errors.get())
            val queueSize = tracker.queueSize()
            assertEquals(
                "All 10 unique impressions should be in queue, got $queueSize — " +
                    "concurrent tracking lost events",
                10,
                queueSize,
            )
        }

    /**
     * Concurrent cleanup + tracking must not throw ConcurrentModificationException.
     *
     * While cleanupOldSignatures() iterates the map, track() must be able
     * to read/write safely.
     */
    @Test
    fun `concurrentCleanupAndTrackingDoesNotThrow()`() =
        runTest {
            val tracker = AnalyticsTracker.getInstance(config)

            // Seed with impressions
            for (i in 0 until 20) {
                tracker.trackImpression("video_$i", "Row", i)
            }

            val latch = CountDownLatch(1)
            val errors = AtomicInteger(0)

            // Thread 1: keep calling cleanup
            val cleanupThread =
                Thread {
                    latch.await()
                    repeat(50) {
                        try {
                            tracker.cleanupSignatures()
                        } catch (e: Exception) {
                            errors.incrementAndGet()
                        }
                        Thread.sleep(1)
                    }
                }

            // Thread 2: keep tracking
            val trackingThread =
                Thread {
                    latch.await()
                    repeat(500) {
                        try {
                            tracker.trackImpression("video_concurrent", "Row", 0)
                        } catch (e: Exception) {
                            errors.incrementAndGet()
                        }
                        Thread.sleep(1)
                    }
                }

            cleanupThread.start()
            trackingThread.start()

            // Let them run concurrently
            Thread.sleep(2000)

            trackingThread.interrupt()
            cleanupThread.interrupt()

            cleanupThread.join(2000)
            trackingThread.join(2000)

            assertEquals(
                "No exceptions should be thrown during concurrent cleanup + tracking",
                0,
                errors.get(),
            )
        }
}
