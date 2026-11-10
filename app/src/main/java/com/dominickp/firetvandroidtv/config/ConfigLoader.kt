package com.dominickp.firetvandroidtv.config

import com.dominickp.firetvandroidtv.FireTVApplication
import com.dominickp.firetvandroidtv.data.network.createFeedApi
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.serialization.json.Json

/**
 * ConfigLoader — loads app_config.json from assets.
 * Mirrors Roku's ConfigManager.xml + ConfigLoader.bs behavior.
 *
 * In Roku, config is loaded at channel start from pkg:/config/default.json.
 * Here, we load from assets/app_config.json at app startup.
 */
open class ConfigLoader(
    private val assetManager: android.content.res.AssetManager? = null,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private val appConfig: AppConfig by lazy { loadConfig() }

    /**
     * Load the full config from the bundled JSON file.
     * Accepts an optional AssetManager for testability; falls back to Application instance if null.
     */
    open fun loadConfig(assetManager: android.content.res.AssetManager? = null): AppConfig {
        try {
            val assets = assetManager ?: FireTVApplication.application.assets
            val inputStream = assets.open("app_config.json")
            val jsonString = inputStream.bufferedReader().readText()
            val config = json.decodeFromString<AppConfig>(jsonString)
            return config
        } catch (e: Exception) {
            Logger.e("ConfigLoader", "Failed to load config: ${e.message}")
            return loadFallbackConfig()
        }
    }

    /**
     * Get the full configuration.
     */
    fun getConfig(): AppConfig = appConfig

    /**
     * Get individual config sections (like Roku ConfigManager SG fields).
     */
    fun getBrandingConfig(): BrandingConfig = appConfig.branding

    fun getContentConfig(): ContentConfig = appConfig.content

    fun getFeaturesConfig(): FeaturesConfig = appConfig.features

    fun getNavItems(): List<NavItem> = appConfig.navItems

    fun getPageRegistry(): Map<String, PageConfig> = appConfig.pageRegistry

    fun getUiConfig(): UiConfig = appConfig.ui

    fun getCardStyle(): CardStyleConfig = appConfig.cardStyle

    fun getAnalyticsConfig(): AnalyticsConfig = appConfig.analytics

    fun getGivingConfig(): GivingConfig = appConfig.giving

    fun getHeartbeatConfig(): HeartbeatConfig = appConfig.heartbeat

    /**
     * Create a FeedRepository for fetching the content feed.
     * Uses the server URL and config values (retry, timeout, user-agent) from config.
     */
    fun createFeedRepository(): com.dominickp.firetvandroidtv.data.network.FeedRepository {
        val baseUrl = appConfig.apiBaseUrl
        val contentConfig = appConfig.content
        return com.dominickp.firetvandroidtv.data.network.FeedRepository(
            createFeedApi(baseUrl, contentConfig),
            contentConfig,
        )
    }
}

/**
 * Fallback config — used when app_config.json is missing.
 * Matches the demo-starter.json defaults from roku-channel-template.
 */
fun loadFallbackConfig(): AppConfig =
    AppConfig(
        channel =
            ChannelConfig(
                name = "FireTV App",
                description = "Video streaming for Android TV and Fire TV",
                language = "en",
            ),
        content =
            ContentConfig(
                sourceUrl = "https://example.invalid/feed",
                userAgent = "FireTV/1.0",
                maxRows = 6,
                maxItemsPerRow = 12,
                retryAttempts = 3,
                retryDelayMs = 2000,
                fetchTimeout = 30,
            ),
        navItems =
            listOf(
                NavItem(id = "home", label = "Home", icon = "Home", page = "home", order = 0),
                NavItem(id = "search", label = "Search", icon = "Search", page = "search", order = 1),
                NavItem(id = "continue", label = "Continue", icon = "Continue", page = "continue", order = 2),
                NavItem(id = "settings", label = "Settings", icon = "Settings", page = "settings", order = 3),
            ),
    )
