package com.dominickp.firetvandroidtv

import com.dominickp.firetvandroidtv.util.HeartbeatService
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * FireTVApplicationLifecycleTest — regression coverage for the low-memory
 * lifecycle path in [FireTVApplication].
 *
 * Regression: `onLowMemory()` stopped the heartbeat but never cleared the
 * `isForeground` flag, unlike `onStop()` (which does). The `onStart()`
 * observer only re-starts the heartbeat when `if (!isForeground)` — so a
 * low-memory trim left `isForeground` stuck `true` while the heartbeat was
 * stopped. On the next background → foreground transition, `onStart()` saw a
 * stale `true` and skipped the re-start, so the session keep-alive never
 * resumed.
 *
 * This test pins the invariant: stopping the heartbeat via a low-memory trim
 * must clear `isForeground` exactly the way stopping via `onStop()` does. It
 * is fully hermetic (no live network, no Activity lifecycle timing) so it is
 * deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FireTVApplicationLifecycleTest {
    @Test
    fun `onLowMemory clears isForeground like onStop so the heartbeat can resume`() {
        val app = FireTVApplication()

        // Simulate the app being in the foreground with a live heartbeat — the
        // state produced by ProcessLifecycleOwner.onStart -> heartbeatService.start().
        val server = MockWebServer().apply { start() }
        val service = HeartbeatService(server.url("/hb").toString(), 1)
        ReflectionHelpers.setField(app, "heartbeatService", service)
        ReflectionHelpers.setField(app, "isForeground", true)

        // A low-memory trim arrives while the app is still foreground: the
        // heartbeat is stopped to free threads/connections. (stop() sets
        // running=false synchronously, so no main-looper pumping is needed.)
        app.onLowMemory()

        // Behavioral: the heartbeat must actually be stopped (freed) on low memory.
        assertFalse(
            "onLowMemory() must stop the heartbeat to free resources",
            ReflectionHelpers.getField(service, "running") as Boolean,
        )

        // Regression (the fix under test): it must ALSO clear the foreground
        // flag, exactly like onStop(). Without this, onStart()'s `if
        // (!isForeground)` gate stays blocked and the heartbeat never resumes.
        val foreground = ReflectionHelpers.getField(app, "isForeground") as Boolean
        assertFalse(
            "onLowMemory() must set isForeground=false (matching onStop) — " +
                "a stale true leaves the heartbeat's resume gate blocked; " +
                "got isForeground=$foreground",
            foreground,
        )

        // Cleanup: restore flag, release the mock server. (service was already
        // stopped by onLowMemory.)
        ReflectionHelpers.setField(app, "isForeground", true)
        server.shutdown()
    }
}
