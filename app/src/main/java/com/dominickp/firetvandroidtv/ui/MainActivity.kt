package com.dominickp.firetvandroidtv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.dominickp.firetvandroidtv.BuildConfig
import com.dominickp.firetvandroidtv.config.ConfigLoader
import com.dominickp.firetvandroidtv.data.local.SettingsPreferences
import com.dominickp.firetvandroidtv.ui.navigation.AppNavigation
import com.dominickp.firetvandroidtv.ui.navigation.NavBridge
import com.dominickp.firetvandroidtv.ui.navigation.RemoteKeyEventBus
import com.dominickp.firetvandroidtv.ui.navigation.RemoteKeyHandler
import com.dominickp.firetvandroidtv.ui.splash.SplashScreen
import com.dominickp.firetvandroidtv.ui.theme.BrandTheme
import com.dominickp.firetvandroidtv.util.Logger

/**
 * MainActivity — the entry point for the FireTV Android TV app.
 *
 * Architecture: MainActivity → BrandTheme → AppNavigation → [LeftNav, Search, Settings, Player]
 *
 * Responsibilities:
 * 1. Load configuration from assets/app_config.json (with fallback)
 * 2. Create FeedViewModel to manage feed data
 * 3. Render AppNavigation which handles all screen routing
 * 4. Handle deep links (watch?content_id=xxx)
 * 5. Show splash screen during initialization
 */
class MainActivity : ComponentActivity() {
    // Activity-level state for exit dialog — bridged between onBackPressed callback and Compose
    // Using a plain Boolean var (not mutableStateOf) since this runs outside Compose context
    private var showExitDialog = false
    private var onExitConfirmed: (() -> Unit)? = null
    private var remoteKeyEventBus: RemoteKeyEventBus = RemoteKeyEventBus()
    // Set by AppNavigation via NavBridge — called when Back should navigate to Home from a sub-screen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The "Keep screen awake" setting (default ON) is applied reactively inside
        // setContent below — it adds/removes FLAG_KEEP_SCREEN_ON on the window as the
        // user toggles it in Settings → Display. On Fire TV the stick's display can
        // otherwise go to sleep while the app is open (and that also drops the
        // ADB-over-WiFi link, making the device look "offline").

        // Exit confirmation dialog — handles all back presses:
        //   - From Home: show exit dialog (confirm before closing app)
        //   - From sub-screen (Search/Settings/Video): navigate to Home via
        //     onNavigateBackToHome callback (set by AppNavigation). This is done
        //     by calling super dispatch to let AppNavigation's internal route
        //     handler update currentRoute, but we intercept BEFORE the exit dialog
        //     by checking FocusManager.currentScreen.
        //
        // Strategy: Always intercept Back. Check current screen:
        //   - If on sub-screen → navigate to Home (via callback)
        //   - If on Home → show exit dialog
        val exitCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val onBack = NavBridge.onBackToHome
                    if (onBack != null) {
                        // On sub-screen (Search/Settings/Video) → navigate to Home
                        onBack()
                        // Keep callback enabled to handle further back presses
                    } else {
                        // On Home screen (or NavBridge not yet set) → exit dialog
                        showExitDialog = true
                        isEnabled = false
                    }
                }
            }
        onBackPressedDispatcher.addCallback(this, exitCallback)

        @androidx.media3.common.util.UnstableApi
        setContent {
            // Load config once at startup with defensive error handling
            val configLoader = remember { ConfigLoader() }
            val appConfig = remember { configLoader.getConfig() }

            // Create FeedViewModel — it creates its own FeedRepository internally
            val feedViewModel: FeedViewModel =
                viewModel {
                    FeedViewModel(configLoader)
                }

            // Collect feed data
            // Note: collectAsState() is acceptable here — MainActivity is the root Activity and
            // collection is cancelled when setContent is torn down (Activity destroy).
            val feedUiState by feedViewModel.uiState.collectAsState()

            // Handle deep link if present in intent
            val deepLinkVideoId = remember { extractDeepLinkId(intent) }

            // Track deep link resolution state to prevent race conditions
            var deepLinkHandled by remember { mutableStateOf(false) }

            // Resolve deep link video ID against loaded feed
            val resolvedDeepLinkVideo =
                remember {
                    mutableStateOf<com.dominickp.firetvandroidtv.data.model.VideoItem?>(null)
                }

            // Resolve deep link when feed loads successfully — from initial intent
            // Handles race conditions: only resolves once, handles feed failure gracefully
            // Key on stable values: deepLinkVideoId + feed state class name + groups size
            val feedStateKey =
                remember(feedUiState) {
                    Pair(
                        feedUiState::class.simpleName,
                        (feedUiState as? FeedUiState.Success)?.groups?.size,
                    )
                }
            LaunchedEffect(deepLinkVideoId, feedStateKey, deepLinkHandled) {
                if (deepLinkHandled) return@LaunchedEffect
                deepLinkVideoId?.let { videoId ->
                    when (feedUiState) {
                        is FeedUiState.Success -> {
                            val resolved = feedViewModel.resolveDeepLink(videoId)
                            if (resolved != null) {
                                resolvedDeepLinkVideo.value = resolved
                            }
                            deepLinkHandled = true
                        }

                        is FeedUiState.Error -> {
                            // Feed failed — don't reset handled flag, user will see error screen
                            deepLinkHandled = true
                        }

                        else -> {} // Loading — wait for Success or Error
                    }
                }
            }

            // Splash screen state
            var showSplash by remember { mutableStateOf(true) }

            // Dismiss splash when feed loads successfully or encounters error
            // Key on stable feed state class name to avoid unnecessary restarts
            val splashKey = remember(feedUiState) { feedUiState::class.simpleName }
            LaunchedEffect(splashKey) {
                if (feedUiState !is FeedUiState.Loading) {
                    showSplash = false
                }
            }

            // Apply / remove FLAG_KEEP_SCREEN_ON based on the "Keep screen awake"
            // setting (default ON). LaunchedEffect re-runs only when the value
            // changes, so the window flag tracks the user's toggle live.
            val keepScreenAwake by remember {
                SettingsPreferences(this).keepScreenAwake
            }.collectAsState(initial = true)
            LaunchedEffect(keepScreenAwake) {
                if (keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Render the full app with theme and navigation
            BrandTheme(colors = appConfig.branding.colors) {
                if (showSplash) {
                    SplashScreen(
                        channelName = appConfig.channel.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        // Show deep link video if resolved
                        val openUrl: (String) -> Unit = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            startActivity(intent)
                        }
                        resolvedDeepLinkVideo.value?.let { video ->
                            AppNavigation(
                                config = appConfig,
                                feedUiState = feedUiState,
                                onRetryFeed = { feedViewModel.retry() },
                                deepLinkVideo = video,
                                onOpenUrl = openUrl,
                                remoteKeyEventBus = remoteKeyEventBus,
                            )
                        } ?: AppNavigation(
                            config = appConfig,
                            feedUiState = feedUiState,
                            onRetryFeed = { feedViewModel.retry() },
                            onOpenUrl = openUrl,
                            remoteKeyEventBus = remoteKeyEventBus,
                        )

                        // Exit confirmation dialog overlay (mirrors Roku exit dialog)
                        if (showExitDialog) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showExitDialog = false
                                        exitCallback.isEnabled = true
                                    },
                                    title = { Text("Exit App?") },
                                    text = { Text("Are you sure you want to exit?") },
                                    confirmButton = {
                                        Button(onClick = {
                                            showExitDialog = false
                                            onExitConfirmed?.invoke() ?: finishAndRemoveTask()
                                        }) {
                                            Text("Yes")
                                        }
                                    },
                                    dismissButton = {
                                        Button(onClick = {
                                            showExitDialog = false
                                            exitCallback.isEnabled = true
                                        }) {
                                            Text("No")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Deep links are handled in onCreate via the initial intent.
        // For future enhancement, a lifecycle-aware approach could handle
        // deep links that arrive while the app is already running.
    }

    /**
     * Intercept hardware remote key events for TV-specific navigation.
     *
     * Fire TV voice button (KEYCODE_SEARCH / KEYCODE_VOICE_ASSIST) triggers
     * navigation to the Search screen. Channel keys can trigger quick-nav
     * between sidebar items.
     *
     * Returns true if the key was handled by the app, false to pass through
     * to normal Android/Compose event dispatch.
     *
     * Note: super.dispatchKeyEvent is a RestrictedApi per Android lint;
     * the override is accepted in lint-xml-config.xml because it's
     * required for Fire TV remote hardware key handling.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val action = RemoteKeyHandler.handleKey(event.keyCode, event.action)
        if (action != null) {
            remoteKeyEventBus.emit(action)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Extract content_id from deep link URL.
     * Format: https://example.invalid/watch?content_id=xxx
     */
    internal fun extractDeepLinkId(
        intent: Intent?,
        expectedHost: String = BuildConfig.DEEP_LINK_HOST,
    ): String? {
        val data = intent?.data
        val host = data?.host
        val path = data?.path
        if (host != expectedHost || path != "/watch") return null
        return data?.getQueryParameter("content_id")
    }
}
