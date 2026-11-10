package com.dominickp.firetvandroidtv

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.runner.RunWith

/**
 * Base E2E test class for FireTV Android TV app.
 * Provides application context and initializes FireTVApplication instance
 * for instrumented tests running on device/emulator.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
abstract class BaseE2ETest {
    protected lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        assertNotNull("Application context should not be null", application)
    }
}
