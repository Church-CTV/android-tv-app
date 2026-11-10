package com.dominickp.firetvandroidtv.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.dominickp.firetvandroidtv.analytics.AnalyticsTracker
import com.dominickp.firetvandroidtv.config.AppConfig
import com.dominickp.firetvandroidtv.config.NavItem
import com.dominickp.firetvandroidtv.data.local.SettingsPreferences
import com.dominickp.firetvandroidtv.data.model.VideoItem
import com.dominickp.firetvandroidtv.ui.FeedUiState
import com.dominickp.firetvandroidtv.ui.focus.FocusManager
import com.dominickp.firetvandroidtv.ui.grid.GridScreen
import com.dominickp.firetvandroidtv.ui.leftnav.LeftNavScreen
import com.dominickp.firetvandroidtv.ui.player.VideoPlayerScreen
import com.dominickp.firetvandroidtv.ui.search.SearchScreen
import com.dominickp.firetvandroidtv.ui.settings.SettingsScreen
import com.dominickp.firetvandroidtv.ui.settings.SettingsViewModel
import com.dominickp.firetvandroidtv.ui.subscriptions.SubscriptionScreen
import com.dominickp.firetvandroidtv.util.Logger
import kotlinx.coroutines.withContext

/**
 * AppNavigation — Main navigation controller for the app.
 *
 * Mirrors Roku's scene management from roku-channel-template:
 *   - HomeScene (LeftNavScreen with grouped content)
 *   - SearchScene (SearchScreen, full-screen)
 *   - VideoScene (VideoPlayerScreen, full-screen)
 *   - SettingsScene (SettingsScreen, full-screen)
 *   - ErrorScene (ErrorScreen, full-screen)
 *
 * Navigation architecture:
 *   - LeftNavScreen is rendered ONLY when currentRoute is Home AND layout is "leftnav".
 *     It contains the sidebar + home content area.
 *   - GridScreen is rendered when currentRoute is Home AND layout is "grid".
 *     It contains overhang + full-screen content rows (no sidebar).
 *   - Selecting a sidebar nav item changes currentRoute to the target screen.
 *   - Non-home screens are rendered full-screen by AppNavigation.
 *   - Each non-home screen has a back button that returns to Home.
 *   - This ensures a single source of truth for route state, focus tracking,
 *     analytics, and video save/restore.
 */
@Composable
@androidx.media3.common.util.UnstableApi
fun AppNavigation(
    config: AppConfig,
    feedUiState: FeedUiState,
    modifier: Modifier = Modifier,
    onRetryFeed: () -> Unit = {},
    deepLinkVideo: VideoItem? = null,
    onOpenUrl: (String) -> Unit = {},
    remoteKeyEventBus: RemoteKeyEventBus? = null,
) {
    var currentRoute by remember { mutableStateOf<NavigationRoute>(NavigationRoute.Home) }
    var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }
    var deepLinkHandled by remember { mutableStateOf(false) }
    var deepLinkVideoId by remember { mutableStateOf<String?>(null) }

    val focusManager = FocusManager.getInstance()
    // AnalyticsTracker is a singleton — remember to avoid calling getInstance() on every recomposition.
    // getInstance() is double-checked-locked, but remember eliminates the synchronized call entirely.
    val analyticsTracker = remember { AnalyticsTracker.getInstance() }

    // Layout preference from DataStore (mirrors Roku's configManager.layoutPreference).
    // Changes in SettingsScreen flow back here automatically via the same DataStore.
    // SettingsPreferences is a lightweight wrapper (single dataStore delegate), so
    // constructing it per-remember is safe and matches existing usage patterns.
    // collectAsState() handles the Flow's lifecycle (auto-cancels on disposal).
    val context = LocalContext.current
    val layoutPreference by remember {
        SettingsPreferences(context).layoutPreference
    }.collectAsState(initial = "leftnav")

    // Track screen transitions in FocusManager
    LaunchedEffect(currentRoute) {
        focusManager.currentScreen = currentRoute.screenName
    }

    // Wire NavBridge so hardware Back from a sub-screen navigates to Home
    // instead of closing the app. Set when not on Home; null when on Home
    // so the back callback shows the exit dialog instead.
    LaunchedEffect(currentRoute) {
        NavBridge.onBackToHome =
            if (currentRoute is NavigationRoute.Home) {
                null
            } else {
                {
                    focusManager.restoreFocus()
                    currentRoute = NavigationRoute.Home
                    selectedVideo = null
                }
            }
    }

    // Save focus before entering video player
    LaunchedEffect(selectedVideo, currentRoute) {
        if (currentRoute is NavigationRoute.Video && selectedVideo != null) {
            focusManager.saveFocusState(
                focusManager.currentScreen ?: "home",
                focusManager.lastFocusedItemId,
            )
        }
    }

    // Listen for hardware remote key events (voice/search, channel keys)
    // and navigate accordingly. Voice search key navigates to Search screen.
    remoteKeyEventBus?.let { bus ->
        LaunchedEffect(bus) {
            bus.events.collect { action ->
                when (action) {
                    RemoteKeyAction.VoiceSearch -> {
                        if (currentRoute !is NavigationRoute.Search) {
                            currentRoute = NavigationRoute.Search
                            Logger.d("AppNavigation", "Voice key → Search")
                        }
                    }
                    RemoteKeyAction.NextChannel,
                    RemoteKeyAction.PreviousChannel -> {
                        // Channel keys reserved for future row navigation
                        Logger.d("AppNavigation", "Channel key received: $action")
                    }
                }
            }
        }
    }

    // Track Home impressions asynchronously — do NOT block UI thread
    // Key on stable boolean flags + groups size (not unstable FeedUiState instance)
    var homeImpressionTracked by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute, feedUiState::class.simpleName, (feedUiState as? FeedUiState.Success)?.groups?.size) {
        if (currentRoute is NavigationRoute.Home && !homeImpressionTracked &&
            feedUiState is FeedUiState.Success
        ) {
            // Move impression tracking off the main thread
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                feedUiState.groups.forEach { (rowLabel, videos) ->
                    videos.forEachIndexed { index, video ->
                        analyticsTracker.trackImpression(video.id, rowLabel, index)
                    }
                }
            }
            homeImpressionTracked = true
        }
    }

    // Auto-navigate to Error screen when feed fails.
    // Key on errorState directly (null when no error, stable instance when present).
    // LaunchedEffect must NOT be inside a conditional block — that's a Compose anti-pattern
    // causing unexpected effect restarts. Instead, key on the nullable value and guard inside.
    val errorState = feedUiState as? FeedUiState.Error
    LaunchedEffect(errorState) {
        if (errorState != null && currentRoute !is NavigationRoute.Error) {
            currentRoute = NavigationRoute.Error(errorState.message)
        }
    }

    // Auto-navigate to deep link video when provided
    LaunchedEffect(deepLinkVideo, deepLinkHandled) {
        if (deepLinkVideo != null && !deepLinkHandled) {
            selectedVideo = deepLinkVideo
            deepLinkVideoId = deepLinkVideo.id
            currentRoute = NavigationRoute.Video(deepLinkVideo)
            deepLinkHandled = true
            // Play event is tracked by VideoPlayerScreen on actual playback start.
            // Tracking here caused duplicate events for deep links (same bug as card selection).
        }
    }

    // Main navigation content
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val navParams =
            NavigationParams(
                currentRoute = currentRoute,
                selectedVideo = selectedVideo,
                config = config,
                feedUiState = feedUiState,
                errorState = errorState,
                deepLinkVideoId = deepLinkVideoId,
                layoutPreference = layoutPreference,
                onCardSelected = { video ->
                    focusManager.lastFocusedItemId = "card_${video.id}"
                    selectedVideo = video
                    currentRoute = NavigationRoute.Video(video)
                    // Play event is tracked by VideoPlayerScreen on actual playback start.
                    // Tracking here caused duplicate events (navigation + player start).
                },
                onBack = {
                    when (currentRoute) {
                        is NavigationRoute.Video -> {
                            focusManager.restoreFocus()
                            currentRoute = NavigationRoute.Home
                            selectedVideo = null
                        }
                        is NavigationRoute.Error -> {
                            currentRoute = NavigationRoute.Home
                        }
                        else -> {
                            currentRoute = NavigationRoute.Home
                        }
                    }
                },
                onSidebarNav = { page ->
                    currentRoute = page.toNavigationRoute()
                },
                onRetryFeed = onRetryFeed,
                onOpenUrl = onOpenUrl,
            )
        NavigationContent(navParams)
    }
}

/**
 * Bundled navigation parameters for NavigationContent.
 * Reduces parameter count to stay under detekt's LongParameterList threshold.
 */
private data class NavigationParams(
    val currentRoute: NavigationRoute,
    val selectedVideo: VideoItem?,
    val config: AppConfig,
    val feedUiState: FeedUiState,
    val errorState: FeedUiState.Error?,
    val deepLinkVideoId: String?,
    val layoutPreference: String,
    val onCardSelected: (VideoItem) -> Unit,
    val onBack: () -> Unit,
    val onSidebarNav: (String) -> Unit,
    val onRetryFeed: () -> Unit,
    val onOpenUrl: (String) -> Unit,
)

/**
 * NavigationContent — renders the appropriate screen based on currentRoute.
 * Extracted from AppNavigation to reduce cyclomatic complexity.
 */
@androidx.media3.common.util.UnstableApi
@Composable
private fun NavigationContent(params: NavigationParams) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel> {
            val settingsPrefs = com.dominickp.firetvandroidtv.data.local.SettingsPreferences(context)
            SettingsViewModel(settingsPrefs)
        }

    when (params.currentRoute) {
        is NavigationRoute.Home -> {
            // Layout preference routing (mirrors Roku's MainScene layout switch):
            //   "grid"    → GridScreen (overhang + full-screen rows, no sidebar)
            //   otherwise → LeftNavScreen (sidebar + content area)
            val homeGroups =
                when (params.feedUiState) {
                    is FeedUiState.Success -> params.feedUiState.groups
                    else -> emptyMap()
                }
            if (params.layoutPreference == "grid") {
                GridScreen(
                    groupedVideos = homeGroups,
                    onCardSelected = params.onCardSelected,
                    config = params.config,
                )
            } else {
                val navItems = if (params.config.navItems.isNotEmpty()) params.config.navItems else defaultNavItems
                LeftNavScreen(
                    navItems = navItems,
                    groupedVideos = homeGroups,
                    onCardSelected = params.onCardSelected,
                    onNavRouteChange = params.onSidebarNav,
                    config = params.config,
                )
            }
        }
        is NavigationRoute.Search -> {
            // Shell mode: the left nav stays visible; search renders in the
            // content slot. LEFT at the keyboard's left edge re-focuses the
            // sidebar's "search" item (LeftNavScreen.focusPageInSidebar).
            val navItems = if (params.config.navItems.isNotEmpty()) params.config.navItems else defaultNavItems
            val homeGroups =
                when (params.feedUiState) {
                    is FeedUiState.Success -> params.feedUiState.groups
                    else -> emptyMap()
                }
            LeftNavScreen(
                navItems = navItems,
                groupedVideos = homeGroups,
                onCardSelected = params.onCardSelected,
                onNavRouteChange = params.onSidebarNav,
                config = params.config,
                page = "search",
                content = { onFocusSidebar, contentAnchor ->
                    SearchScreen(
                        onCardSelected = params.onCardSelected,
                        onFocusSidebar = onFocusSidebar,
                        contentAnchor = contentAnchor,
                    )
                },
            )
        }
        is NavigationRoute.Video -> {
            params.selectedVideo?.let { video ->
                val referrer =
                    if (video.id == params.deepLinkVideoId) {
                        "deepLink"
                    } else {
                        "video"
                    }
                VideoPlayerScreen(
                    videoItem = video,
                    onBack = params.onBack,
                    referrer = referrer,
                )
            }
        }
        is NavigationRoute.Settings -> {
            val navItems = if (params.config.navItems.isNotEmpty()) params.config.navItems else defaultNavItems
            val homeGroups =
                when (params.feedUiState) {
                    is FeedUiState.Success -> params.feedUiState.groups
                    else -> emptyMap()
                }
            LeftNavScreen(
                navItems = navItems,
                groupedVideos = homeGroups,
                onCardSelected = params.onCardSelected,
                onNavRouteChange = params.onSidebarNav,
                config = params.config,
                page = "settings",
                content = { onFocusSidebar, contentAnchor ->
                    SettingsScreen(
                        config = params.config,
                        viewModel = settingsViewModel,
                        onBack = params.onBack,
                        onOpenUrl = params.onOpenUrl,
                        onFocusSidebar = onFocusSidebar,
                        contentAnchor = contentAnchor,
                    )
                },
            )
        }
        is NavigationRoute.Subscription -> {
            SubscriptionScreen(
                onBack = params.onBack,
            )
        }
        is NavigationRoute.Error -> {
            val errMsg = params.errorState?.message ?: "Unknown error"
            ErrorScreen(
                message = errMsg,
                onRetry = params.onRetryFeed,
            )
        }
    }
}

/**
 * Converts a page string to the corresponding NavigationRoute.
 */
internal fun String.toNavigationRoute(): NavigationRoute =
    when (this) {
        "home" -> NavigationRoute.Home
        "search" -> NavigationRoute.Search
        "settings" -> NavigationRoute.Settings
        "subscription" -> NavigationRoute.Subscription
        else -> NavigationRoute.Home
    }

/**
 * Navigation routes for the app.
 */
sealed interface NavigationRoute {
    data object Home : NavigationRoute

    data object Search : NavigationRoute

    data class Video(
        val video: VideoItem,
    ) : NavigationRoute

    data object Settings : NavigationRoute

    data object Subscription : NavigationRoute

    data class Error(
        val message: String,
    ) : NavigationRoute
}

/**
 * Extension to get the screen name for FocusManager tracking.
 */
internal val NavigationRoute.screenName: String
    get() =
        when (this) {
            is NavigationRoute.Home -> "home"
            is NavigationRoute.Search -> "search"
            is NavigationRoute.Video -> "video"
            is NavigationRoute.Settings -> "settings"
            is NavigationRoute.Subscription -> "subscription"
            is NavigationRoute.Error -> "error"
        }

/**
 * Default navigation items.
 */
private val defaultNavItems: List<NavItem> =
    listOf(
        NavItem(id = "home", label = "Home", page = "home", visible = true),
        NavItem(id = "search", label = "Search", page = "search", visible = true),
        NavItem(id = "settings", label = "Settings", page = "settings", visible = true),
    )
