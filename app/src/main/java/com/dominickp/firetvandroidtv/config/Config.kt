package com.dominickp.firetvandroidtv.config

import kotlinx.serialization.Serializable

/**
 * Configuration data classes — 1:1 mapping with Roku `default.json` schema.
 * Mirrors the complete config structure from roku-channel-template.
 */

// === Channel ===
@Serializable
data class ChannelConfig(
    val name: String,
    val description: String = "",
    val language: String = "en",
    val copyright: String = "",
)

// === Branding ===
@Serializable
data class BrandingConfig(
    val colors: ColorsConfig = ColorsConfig(),
    val images: ImagesConfig = ImagesConfig(),
    val fonts: FontsConfig = FontsConfig(),
    val spacing: SpacingConfig = SpacingConfig(),
    val animation: AnimationConfig = AnimationConfig(),
)

@Serializable
data class ColorsConfig(
    val background: String = "#1B5E20",
    val surface: String = "#132038",
    val accent: String = "#4CAF50",
    val textPrimary: String = "#F8FAFC",
    val textSecondary: String = "#A0A0B0",
    val focus: String = "#00D4AA",
    val error: String = "#FF5C5C",
    val navFocusBg: String = "#000000",
)

@Serializable
data class ImagesConfig(
    val overhangLogo: String = "",
    val backgroundImage: String = "",
    val iconFocus: String = "pkg:/images/icon_focus_hd.jpg",
    val liveStreamThumbnail: String = "",
)

@Serializable
data class FontsConfig(
    val title: String = "font:BoldSystemFont",
    val body: String = "font:MediumSystemFont",
    val caption: String = "font:SmallSystemFont",
    val heading: String = "font:ExtraBoldSystemFont",
)

@Serializable
data class SpacingConfig(
    val marginHorizontal: Int = 80,
    val marginVertical: Int = 40,
)

@Serializable
data class AnimationConfig(
    val cardScale: Float = 1.04f,
    val transitionDuration: Double = 0.2,
)

// === Content (feed) ===
@Serializable
data class ContentConfig(
    val sourceUrl: String,
    val userAgent: String = "FireTV/1.0",
    val maxRows: Int = 6,
    val maxItemsPerRow: Int = 12,
    val retryAttempts: Int = 3,
    val retryDelayMs: Int = 2000,
    val fetchTimeout: Int = 30,
    val sslCertificates: String = "common:/certs/ca-bundle.crt",
)

// === Features ===
@Serializable
data class FeaturesConfig(
    val layoutPreference: String = "leftnav",
    val search: Boolean = true,
    val deepLinking: Boolean = true,
    val analytics: Boolean = false,
    val crashReporting: Boolean = true,
    val overhangLogo: Boolean = true,
    val overhangClock: Boolean = false,
    val overhangOptions: Boolean = false,
    val continueWatching: Boolean = false,
    val liveService: Boolean = false,
    val videoPlayerScreen: Boolean = true,
)

// === Navigation ===
@Serializable
data class NavItem(
    val id: String,
    val label: String,
    val icon: String = "",
    val page: String = "",
    val visible: Boolean = true,
    val order: Int = 0,
)

// === Page Registry (maps page ID → component name, like Roku pageRegistry) ===
@Serializable
data class PageConfig(
    val component: String,
    val title: String = "",
    val requiresAuth: Boolean = false,
)

// === UI ===
@Serializable
data class UiConfig(
    val backgroundColor: String = "#1B5E20",
    val layout: String = "leftnav",
    val availableLayouts: List<String> =
        listOf(
            "grid",
            "leftnav",
            "list",
            "pagebased",
            "videoplayer",
            "livetv",
            "category",
        ),
    val welcome: WelcomeConfig = WelcomeConfig(),
    val error: ErrorConfig = ErrorConfig(),
    val loading: LoadingConfig = LoadingConfig(),
)

@Serializable
data class WelcomeConfig(
    val enabled: Boolean = true,
    val title: String = "Loading...",
    val subtitle: String = "Please wait",
)

@Serializable
data class ErrorConfig(
    val title: String = "Connection Error",
    val message: String = "Unable to load content. Please check your network connection and try again.",
    val retryLabel: String = "Retry",
)

@Serializable
data class LoadingConfig(
    val message: String = "Loading...",
)

// === Analytics (Roku client analytics config) ===
@Serializable
data class AnalyticsConfig(
    val enabled: Boolean = false,
    val exportUrl: String = "",
    val exportIntervalSeconds: Int = 300,
    val maxEventsPerBatch: Int = 50,
    val flushOnError: Boolean = true,
)

// === Card Style (mirrors Roku ContentCard interface fields) ===
@Serializable
data class CardStyleConfig(
    val showTitle: Boolean = true,
    val showDescription: Boolean = true,
    val titleFont: String = "font:SmallBoldSystemFont",
    val descriptionFont: String = "font:SmallSystemFont",
    val titleColor: String = "#FFFFFF",
    val descriptionColor: String = "#A0A0B0",
    val titleNumLines: Int = 1,
    val descriptionNumLines: Int = 1,
    val showDetailTitle: Boolean = true,
    val showDetailDescription: Boolean = true,
    val detailTitleFont: String = "font:MediumSystemFont",
    val detailDescriptionFont: String = "font:SmallSystemFont",
    val detailTitleColor: String = "#FFFFFF",
    val detailDescriptionColor: String = "#A0A0B0",
    val detailTitleNumLines: Int = 1,
    val detailDescriptionNumLines: Int = 2,
)

// === Server (optional override for Roku's content.sourceUrl) ===
@Serializable
data class ServerConfig(
    val baseUrl: String = "",
    val timeout: Int = 30,
)

// === Giving (Roku's "giving" config — donation URL) ===
@Serializable
data class GivingConfig(
    val donationUrl: String = "",
)

// === Heartbeat (Roku's heartbeat monitoring) ===
@Serializable
data class HeartbeatConfig(
    val intervalSeconds: Int = 60,
    val exportUrl: String = "",
)

// === ROOT: Full App Config ===
@Serializable
data class AppConfig(
    val version: String = "1.0.0",
    val channel: ChannelConfig,
    val branding: BrandingConfig = BrandingConfig(),
    val content: ContentConfig,
    val features: FeaturesConfig = FeaturesConfig(),
    val navItems: List<NavItem> = emptyList(),
    val pageRegistry: Map<String, PageConfig> = emptyMap(),
    val ui: UiConfig = UiConfig(),
    val analytics: AnalyticsConfig = AnalyticsConfig(),
    val cardStyle: CardStyleConfig = CardStyleConfig(),
    val server: ServerConfig = ServerConfig(),
    val giving: GivingConfig = GivingConfig(),
    val heartbeat: HeartbeatConfig = HeartbeatConfig(),
) {
    /**
     * The base URL for API calls.
     * If server.baseUrl is set, use it. Otherwise derive from content.sourceUrl.
     */
    val apiBaseUrl: String
        get() =
            server.baseUrl.ifBlank {
                // Extract base URL from feed URL (remove /feed path)
                content.sourceUrl.removeSuffix("/feed").removeSuffix("/")
            }
}
