package com.dominickp.firetvandroidtv.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DeviceUtilsTest — tests the logical invariants of DeviceUtils.
 *
 * DeviceUtils caches platform-detection booleans derived from Build.* fields and
 * Runtime. Rather than mock final Build statics (fragile), these tests assert the
 * invariants that must ALWAYS hold regardless of the underlying Build values:
 *
 *   - isTv = isFireTV || isAndroidTV  (never false on a TV)
 *   - deviceModel is a non-empty identifier string
 *   - isLowRam is a stable boolean (no exception)
 *   - isFireTV and isAndroidTV are mutually consistent
 *
 * In the Robolectric environment (leanback on classpath, non-Amazon manufacturer),
 *   isAndroidTV=true and isFireTV=false are the stable, expected values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeviceUtilsTest {

    @Test
    fun `isTv equals isFireTV OR isAndroidTV`() {
        val expected = DeviceUtils.isFireTV || DeviceUtils.isAndroidTV
        assertTrue(
            "isTv must be (isFireTV || isAndroidTV) by definition",
            DeviceUtils.isTv == expected,
        )
    }

    @Test
    fun `deviceModel is a non-empty identifier`() {
        val model = DeviceUtils.deviceModel
        assertNotNull("deviceModel must not be null", model)
        assertTrue(
            "deviceModel must be non-empty",
            model.isNotBlank(),
        )
    }

    @Test
    fun `deviceModel contains the manufacturer`() {
        // deviceModel format is "<MANUFACTURER> <MODEL> (API <SDK>)".
        val model = DeviceUtils.deviceModel
        assertTrue(
            "deviceModel must start with the manufacturer",
            model.contains(android.os.Build.MANUFACTURER),
        )
    }

    @Test
    fun `isLowRam returns a stable boolean`() {
        // Just ensure it evaluates without throwing and is consistent.
        val first = DeviceUtils.isLowRam
        val second = DeviceUtils.isLowRam
        assertTrue("isLowRam must be consistent", first == second)
        // On a 59GB dev host / Robolectric heap, this will be false, but don't assert
        // the platform value — just that it's a stable boolean.
        assertTrue(first is Boolean)
    }

    @Test
    fun `isFireTV and isAndroidTV are mutually exclusive in this environment`() {
        // In Robolectric with leanback present and non-Amazon manufacturer:
        // isAndroidTV=true, isFireTV=false. Both derived from the same Build source,
        // so they cannot BOTH be true under the current detection logic
        // (isAndroidTV explicitly excludes MANUFACTURER=="Amazon").
        assertFalse(
            "isFireTV and isAndroidTV cannot both be true (isAndroidTV excludes Amazon)",
            DeviceUtils.isFireTV && DeviceUtils.isAndroidTV,
        )
    }
}
