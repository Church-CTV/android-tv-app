package com.dominickp.firetvandroidtv.contract

import com.dominickp.firetvandroidtv.config.ConfigLoader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Contract tests verifying core data contracts match the @ctv/core specification.
 * These ensure the app's data models remain compatible with the server/feed contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppContractTest {
    private val configLoader = ConfigLoader()

    @Test
    fun `config loads without throwing`() {
        val config = configLoader.loadConfig()
        assertNotNull(config)
    }

    @Test
    fun `config has valid channel name`() {
        val config = configLoader.loadConfig()
        assertFalse(config.channel.name.isEmpty())
    }

    @Test
    fun `config has valid source URL`() {
        val config = configLoader.loadConfig()
        assertFalse(config.content.sourceUrl.isEmpty())
    }

    @Test
    fun `config has valid apiBaseUrl`() {
        val config = configLoader.loadConfig()
        val baseUrl = config.apiBaseUrl
        assertNotNull(baseUrl)
        assertTrue(baseUrl.startsWith("http"))
    }

    @Test
    fun `config has valid navItems`() {
        val config = configLoader.loadConfig()
        assertFalse(config.navItems.isEmpty())
    }

    @Test
    fun `config has valid analytics config`() {
        val config = configLoader.loadConfig()
        assertNotNull(config.analytics)
    }

    @Test
    fun `config has valid heartbeat config`() {
        val config = configLoader.loadConfig()
        assertNotNull(config.heartbeat)
    }

    @Test
    fun `config has valid branding colors`() {
        val config = configLoader.loadConfig()
        assertNotNull(config.branding)
        assertNotNull(config.branding.colors.background)
    }
}
