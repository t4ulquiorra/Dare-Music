/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.dare.innertube.YouTube
import com.dare.innertube.models.SongItem
import com.dare.innertube.models.WatchEndpoint
import com.dare.music.constants.AppBarHeight
import com.dare.music.constants.AppLanguageKey
import com.dare.music.constants.CheckForUpdatesKey
import com.dare.music.constants.DarkModeKey
import com.dare.music.constants.DefaultOpenTabKey
import com.dare.music.constants.DisableScreenshotKey
import com.dare.music.constants.DynamicThemeKey
import com.dare.music.constants.EnableHighRefreshRateKey
import com.dare.music.constants.LastSeenVersionKey
import com.dare.music.constants.ListenTogetherInTopBarKey
import com.dare.music.constants.ListenTogetherUsernameKey
import com.dare.music.constants.LyricsProviderOrderKey
import com.dare.music.constants.MiniPlayerHeight
import com.dare.music.constants.NavigationBarHeight
import com.dare.music.constants.PauseListenHistoryKey
import com.dare.music.constants.PauseSearchHistoryKey
import com.dare.music.constants.PreferredLyricsProvider
import com.dare.music.constants.PreferredLyricsProviderKey
import com.dare.music.constants.PureBlackKey
import com.dare.music.constants.SYSTEM_DEFAULT
import com.dare.music.constants.SelectedThemeColorKey
import com.dare.music.constants.SimpMusicMigrationDoneKey
import com.dare.music.constants.StopMusicOnTaskClearKey
import com.dare.music.constants.UpdateNotificationsEnabledKey
import com.dare.music.db.MusicDatabase
import com.dare.music.db.entities.SearchHistory
import com.dare.music.extensions.toEnum
import com.dare.music.models.toMediaMetadata
import com.dare.music.playback.DownloadUtil
import com.dare.music.playback.MusicService
import com.dare.music.playback.MusicService.MusicBinder
import com.dare.music.playback.PlayerConnection
import com.dare.music.playback.queues.YouTubeQueue
import com.dare.music.ui.component.AccountSettingsDialog
import com.dare.music.ui.component.AppNavigationRail
import com.dare.music.ui.component.BottomSheetMenu
import com.dare.music.ui.component.BottomSheetPage
import com.dare.music.ui.component.LiquidGlassAppBottomNavigationBar
import com.dare.music.ui.component.LocalBottomSheetPageState
import com.dare.music.ui.component.LocalMenuState
import com.dare.music.ui.component.rememberBottomSheetState
import com.dare.music.ui.component.shimmer.ShimmerTheme
import com.dare.music.ui.menu.YouTubeSongMenu
import com.dare.music.ui.navigation.AppNavigationGraph
import com.dare.music.ui.player.BottomSheetPlayer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.dare.music.ui.screens.Screens
import com.dare.music.ui.screens.settings.ChangelogScreen
import com.dare.music.ui.screens.settings.DarkMode
import com.dare.music.ui.screens.settings.NavigationTab
import com.dare.music.ui.theme.ColorSaver
import com.dare.music.ui.theme.DefaultThemeColor
import com.dare.music.ui.theme.DareTheme
import com.dare.music.ui.theme.extractThemeColor
import com.dare.music.ui.utils.appBarScrollBehavior
import com.dare.music.ui.utils.resetHeightOffset
import com.dare.music.utils.SyncUtils
import com.dare.music.utils.Updater
import com.dare.music.utils.dataStore
import com.dare.music.utils.get
import com.dare.music.utils.rememberEnumPreference
import com.dare.music.utils.rememberPreference
import com.dare.music.utils.reportException
import com.dare.music.utils.setAppLocale
import com.dare.music.viewmodels.HomeViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH       = "com.dare.music.action.SEARCH"
        private const val ACTION_LIBRARY      = "com.dare.music.action.LIBRARY"
        const val ACTION_RECOGNITION          = "com.dare.music.action.RECOGNITION"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
    }

    @Inject lateinit var database:             MusicDatabase
    @Inject lateinit var downloadUtil:         DownloadUtil
    @Inject lateinit var syncUtils:            SyncUtils
    @Inject lateinit var listenTogetherManager: com.dare.music.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection:         PlayerConnection? = null
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                try {
                    playerConnection         = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playerConnectionSnapshot = playerConnection
                    Timber.tag("MainActivity").d("PlayerConnection created")
                    listenTogetherManager.setPlayerConnection(playerConnection)
                } catch (e: Exception) {
                    Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                    lifecycleScope.launch {
                        delay(500)
                        try {
                            playerConnection         = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                            playerConnectionSnapshot = playerConnection
                            listenTogetherManager.setPlayerConnection(playerConnection)
                        } catch (e2: Exception) {
                            Timber.tag("MainActivity").e(e2, "Retry also failed")
                        }
                    }
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
        }
    }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Not bound when unbinding in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
        }
        startService(Intent(this, MusicService::class.java))
        if (!isServiceBound) {
            bindService(Intent(this, MusicService::class.java), serviceConnection, BIND_AUTO_CREATE)
            isServiceBound = true
        }
    }

    override fun onStop() {
        listenTogetherManager.setPlayerConnection(null)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(StopMusicOnTaskClearKey, false) &&
            playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
        }
        playerConnection?.dispose()
        playerConnection         = null
        playerConnectionSnapshot = null
        safeUnbindService("onDestroy()")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) handleDeepLinkIntent(intent, navController)
        else pendingIntent = intent
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data.map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    else    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
        }

        setContent {
            DareApp(
                latestVersionName         = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection          = playerConnectionSnapshot,
                database                  = database,
                downloadUtil              = downloadUtil,
                syncUtils                 = syncUtils,
            )
        }
    }

    // =========================================================================
    // DareApp
    // =========================================================================

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DareApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)

        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (checkForUpdates) {
                    withContext(Dispatchers.IO) {
                        if (!dataStore.get(CheckForUpdatesKey, true)) return@withContext
                        val notifEnabled = dataStore.get(UpdateNotificationsEnabledKey, true)
                        Updater.checkForUpdate().onSuccess { (releaseInfo, hasUpdate) ->
                            if (releaseInfo != null) {
                                onLatestVersionNameChange(releaseInfo.versionName)
                                if (hasUpdate && notifEnabled) {
                                    val url = Updater.getDownloadUrlForCurrentVariant(releaseInfo)
                                    if (url != null) {
                                        val pending = PendingIntent.getActivity(
                                            this@MainActivity, 1001,
                                            Intent(Intent.ACTION_VIEW, url.toUri()),
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                                        )
                                        val notif = NotificationCompat.Builder(this@MainActivity, "updates")
                                            .setSmallIcon(R.drawable.update)
                                            .setContentTitle(getString(R.string.update_available_title))
                                            .setContentText(releaseInfo.versionName)
                                            .setContentIntent(pending)
                                            .setAutoCancel(true)
                                            .build()
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            NotificationManagerCompat.from(this@MainActivity).notify(1001, notif)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                }
            }
        }

        val enableDynamicTheme    by rememberPreference(DynamicThemeKey,          defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey,  defaultValue = true)

        LaunchedEffect(enableHighRefreshRate) {
            val win = this@MainActivity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val lp = win.attributes
                if (enableHighRefreshRate) {
                    lp.preferredDisplayModeId = 0
                } else {
                    val modes  = win.windowManager.defaultDisplay.supportedModes
                    val mode60 = modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                        ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
                    if (mode60 != null) lp.preferredDisplayModeId = mode60.modeId
                }
                win.attributes = lp
            } else {
                val params = win.attributes
                params.preferredRefreshRate = if (enableHighRefreshRate) 0f else 60f
                win.attributes = params
            }
        }

        val darkTheme     by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemDark   = isSystemInDarkTheme()
        val useDarkTheme   = remember(darkTheme, isSystemDark) {
            if (darkTheme == DarkMode.AUTO) isSystemDark else darkTheme == DarkMode.ON
        }
        LaunchedEffect(useDarkTheme) { setSystemBarAppearance(useDarkTheme) }

        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = true)
        val pureBlack         = remember(pureBlackEnabled, useDarkTheme) { pureBlackEnabled && useDarkTheme }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor       = Color(selectedThemeColorInt)
        val showChangelog            = rememberSaveable { mutableStateOf(false) }

        var themeColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(selectedThemeColor) }
        LaunchedEffect(selectedThemeColor) { if (!enableDynamicTheme) themeColor = selectedThemeColor }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            val pc = playerConnection
            if (!enableDynamicTheme || pc == null) { themeColor = selectedThemeColor; return@LaunchedEffect }
            pc.service.currentMediaMetadata.collectLatest { song ->
                if (song?.thumbnailUrl != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val result = imageLoader.execute(
                                ImageRequest.Builder(this@MainActivity)
                                    .data(song.thumbnailUrl).allowHardware(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .networkCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(false).build()
                            )
                            themeColor = result.image?.toBitmap()?.extractThemeColor() ?: selectedThemeColor
                        } catch (_: Exception) { themeColor = selectedThemeColor }
                    }
                } else {
                    themeColor = selectedThemeColor
                }
            }
        }

        DareTheme(darkTheme = useDarkTheme, pureBlack = pureBlack, themeColor = themeColor) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
            ) {
                val density      = LocalDensity.current
                val windowInsets = WindowInsets.systemBars
                val cutoutInsets = WindowInsets.displayCutout
                val bottomInset  = with(density) { windowInsets.getBottom(density).toDp() }

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    if (dataStore.data.first()[SimpMusicMigrationDoneKey] != true) {
                        dataStore.edit { settings ->
                            val cur = settings[LyricsProviderOrderKey] ?: ""
                            if (cur.contains("SimpMusic") || !cur.contains("Paxsenix")) {
                                val list = cur.split(",").map { it.trim() }
                                    .filter { it.isNotBlank() && it != "SimpMusic" }.toMutableList()
                                if (!list.contains("Paxsenix")) list.add("Paxsenix")
                                settings[LyricsProviderOrderKey] = list.joinToString(",")
                            }
                            if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                                settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                            }
                            settings[SimpMusicMigrationDoneKey] = true
                        }
                    }
                    dataStore.edit { settings ->
                        settings[LastSeenVersionKey] = BuildConfig.VERSION_NAME
                        if (settings[com.dare.music.constants.FirstLaunchDateKey] == null) {
                            settings[com.dare.music.constants.FirstLaunchDateKey] = System.currentTimeMillis()
                        }
                    }
                }

                val homeViewModel:  HomeViewModel  = hiltViewModel()
                val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                val navigationItems           = Screens.MainScreens

                val defaultOpenTab = remember {
                    dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                }
                val tabOpenedFromShortcut = remember {
                    when (intent?.action) {
                        ACTION_SEARCH  -> NavigationTab.LIBRARY
                        ACTION_LIBRARY -> NavigationTab.SEARCH
                        else           -> null
                    }
                }

                val topLevelScreens = remember {
                    listOf(Screens.Home.route, Screens.Library.route, Screens.ListenTogether.route, "settings")
                }

                val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }

                val onSearch: (String) -> Unit = remember {
                    { q ->
                        if (q.isNotEmpty()) {
                            navController.navigate("search/${URLEncoder.encode(q, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    database.query { insert(SearchHistory(query = q)) }
                                }
                            }
                        }
                    }
                }

                val currentRoute   by remember { derivedStateOf { navBackStackEntry?.destination?.route } }
                val inSearchScreen by remember { derivedStateOf { currentRoute?.startsWith("search/") == true } }

                val shouldShowNavigationBar = remember(currentRoute) { currentRoute != "wrapped" }
                val showRail                = false

                // isScrolledToTop — set by individual screens via:
                //   navController.currentBackStackEntry?.savedStateHandle?.set("isScrolledToTop", true/false)
                val isScrolledToTop by remember(navBackStackEntry) {
                    derivedStateOf {
                        navBackStackEntry?.savedStateHandle?.get<Boolean>("isScrolledToTop") ?: true
                    }
                }

                // BottomSheetPlayer is expanded-only; DareMiniPlayer (inside the glass
                // nav bar) is the mini-player. collapsedBound = 1.dp keeps the state
                // machine valid while making the collapsed peek effectively invisible.
                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = 1.dp,
                    expandedBound  = maxHeight,
                )

                val playerAwareWindowInsets = remember(
                    bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed,
                ) {
                    var bottom = bottomInset
                    if (shouldShowNavigationBar && !showRail) bottom += NavigationBarHeight
                    if (!playerBottomSheetState.isDismissed)  bottom += MiniPlayerHeight
                    windowInsets
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                }

                val topAppBarScrollBehavior = appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                LaunchedEffect(navBackStackEntry) {
                    if (inSearchScreen) {
                        val raw = navBackStackEntry?.arguments?.getString("query")!!
                        val q   = try { URLDecoder.decode(raw, "UTF-8") } catch (_: IllegalArgumentException) { raw }
                        onQueryChange(TextFieldValue(q, TextRange(q.length)))
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }
                    topAppBarScrollBehavior.state.resetHeightOffset()
                    navController.currentBackStackEntry?.destination?.route?.let { setPreviousTab(it) }
                }

                LaunchedEffect(playerConnection) {
                    val player = playerConnection?.player ?: return@LaunchedEffect
                    if (player.currentMediaItem == null && !playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.dismiss()
                    }
                }

                DisposableEffect(playerConnection, playerBottomSheetState) {
                    val player = playerConnection?.player ?: return@DisposableEffect onDispose {}
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            // Expansion is triggered explicitly by DareMiniPlayer tap
                        }
                    }
                    player.addListener(listener)
                    onDispose { player.removeListener(listener) }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                    val route            = navBackStackEntry?.destination?.route
                    val isListenTogether = route == Screens.ListenTogether.route || route == "listen_together_from_topbar"
                    shouldShowTopBar = route in topLevelScreens &&
                        route != "settings" &&
                        route != Screens.Home.route &&
                        !(isListenTogether && listenTogetherInTopBar)
                }

                val coroutineScope          = rememberCoroutineScope()
                var sharedSong: SongItem? by remember { mutableStateOf(null) }
                val snackbarHostState        = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleRecognitionIntent(pendingIntent!!, navController)
                        handleDeepLinkIntent(pendingIntent!!, navController)
                        pendingIntent = null
                    } else {
                        handleRecognitionIntent(intent, navController)
                        handleDeepLinkIntent(intent, navController)
                    }
                }
                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { i ->
                        handleRecognitionIntent(i, navController)
                        handleDeepLinkIntent(i, navController)
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val currentTitleRes = remember(navBackStackEntry) {
                    when (navBackStackEntry?.destination?.route) {
                        Screens.Home.route          -> null
                        Screens.Search.route        -> R.string.search
                        Screens.Library.route       -> R.string.filter_library
                        Screens.ListenTogether.route -> R.string.together
                        else                        -> null
                    }
                }

                var showAccountDialog  by remember { mutableStateOf(false) }
                val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                val eventCount         by database.eventCount().collectAsState(initial = 0)

                val baseBg        = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                val glassBackdrop = rememberLayerBackdrop()

                CompositionLocalProvider(
                    LocalDatabase               provides database,
                    LocalContentColor           provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection       provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil           provides downloadUtil,
                    LocalShimmerTheme           provides ShimmerTheme,
                    LocalSyncUtils              provides syncUtils,
                    LocalListenTogetherManager  provides listenTogetherManager,
                    LocalChangelogState         provides showChangelog,
                    LocalGlassBackdrop          provides glassBackdrop,
                ) {
                    if (showChangelog.value) {
                        ChangelogScreen(onDismiss = { showChangelog.value = false })
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            AnimatedVisibility(
                                visible = shouldShowTopBar,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit  = fadeOut(animationSpec = tween(200)),
                            ) {
                                Row {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text  = currentTitleRes?.let { stringResource(it) } ?: "",
                                                style = MaterialTheme.typography.titleLarge,
                                            )
                                        },
                                        actions = {
                                            IconButton(onClick = { showAccountDialog = true }) {
                                                BadgedBox(badge = {
                                                    if (latestVersionName != BuildConfig.VERSION_NAME) Badge()
                                                }) {
                                                    if (accountImageUrl != null) {
                                                        AsyncImage(
                                                            model = accountImageUrl,
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier.size(24.dp).clip(CircleShape),
                                                        )
                                                    } else {
                                                        Icon(
                                                            painter = painterResource(R.drawable.account),
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier.size(24.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor         = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            titleContentColor      = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier.windowInsetsPadding(
                                            cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End),
                                        ),
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            if (currentRoute == "wrapped") return@Scaffold

                            val onNavItemClick: (Screens, Boolean) -> Unit = remember(
                                navController, coroutineScope, topAppBarScrollBehavior,
                                playerBottomSheetState, navBackStackEntry,
                            ) {
                                { screen, isSelected ->
                                    if (playerBottomSheetState.isExpanded) playerBottomSheetState.collapseSoft()
                                    if (isSelected) {
                                        val targetEntry = try {
                                            val r = navController.currentBackStackEntry?.destination?.route
                                            if (r == "search/{query}" || r == "search_input") {
                                                navController.getBackStackEntry("search_input")
                                            } else {
                                                navController.currentBackStackEntry
                                            }
                                        } catch (_: Exception) { null }

                                        if (screen == Screens.Search) {
                                            val c = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                            targetEntry?.savedStateHandle?.set("scrollToTopCount", c + 1)
                                        } else {
                                            targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                        }
                                        coroutineScope.launch { topAppBarScrollBehavior.state.resetHeightOffset() }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    }
                                }
                            }

                            Box {
                                // Full-screen player — expanded only.
                                // Tap DareMiniPlayer → onOpenNowPlaying → expand().
                                BottomSheetPlayer(
                                    state         = playerBottomSheetState,
                                    navController = navController,
                                    pureBlack     = pureBlack,
                                    positionState = remember { mutableLongStateOf(0L) },
                                    durationState = remember { mutableLongStateOf(0L) },
                                    backdrop      = glassBackdrop,
                                    modifier      = Modifier,
                                )

                                LiquidGlassAppBottomNavigationBar(
                                    navController    = navController,
                                    backdrop         = glassBackdrop,
                                    bottomNavScreens = navigationItems.take(3),
                                    currentRoute     = currentRoute,
                                    onItemClick      = onNavItemClick,
                                    onOpenNowPlaying = { playerBottomSheetState.expandSoft() },
                                    onStopPlayer     = {
                                        playerConnection?.player?.stop()
                                        playerConnection?.player?.clearMediaItems()
                                    },
                                    isScrolledToTop  = isScrolledToTop,
                                )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            if (showRail && currentRoute != "wrapped") {
                                AppNavigationRail(
                                    navigationItems   = navigationItems,
                                    currentRoute      = currentRoute,
                                    onItemClick       = { screen, isSelected ->
                                        if (playerBottomSheetState.isExpanded) playerBottomSheetState.collapseSoft()
                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch { topAppBarScrollBehavior.state.resetHeightOffset() }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState    = true
                                            }
                                        }
                                    },
                                    pureBlack         = pureBlack,
                                    onSearchLongClick = {
                                        navController.navigate("recognition") { launchSingleTop = true }
                                    },
                                )
                            }

                            Box(Modifier.weight(1f).layerBackdrop(glassBackdrop)) {
                                AppNavigationGraph(
                                    navController     = navController,
                                    startDestination  = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME    -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else                  -> Screens.Home
                                    }.route,
                                    navigationItems   = navigationItems,
                                    scrollBehavior    = topAppBarScrollBehavior,
                                    latestVersionName = latestVersionName,
                                    activity          = this@MainActivity,
                                    snackbarHostState = snackbarHostState,
                                )
                            }
                        }
                    }

                    BottomSheetPlayer(
                        state         = playerBottomSheetState,
                        navController = navController,
                        pureBlack     = pureBlack,
                        positionState = remember { mutableLongStateOf(0L) },
                        durationState = remember { mutableLongStateOf(0L) },
                        backdrop      = glassBackdrop,
                        modifier      = Modifier.align(Alignment.BottomCenter),
                    )

                    BottomSheetMenu(
                        state    = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    BottomSheetPage(
                        state    = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    if (showAccountDialog) {
                        AccountSettingsDialog(
                            navController     = navController,
                            onDismiss         = { showAccountDialog = false; homeViewModel.refresh() },
                            latestVersionName = latestVersionName,
                        )
                    }

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties       = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier       = Modifier.padding(24.dp),
                                    shape          = RoundedCornerShape(16.dp),
                                    color          = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        YouTubeSongMenu(
                                            song          = song,
                                            navController = navController,
                                            onDismiss     = { sharedSong = null },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Intent handlers (unchanged)
    // =========================================================================

    private fun handleRecognitionIntent(intent: Intent, navController: NavHostController) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        intent.removeExtra(EXTRA_AUTO_START_RECOGNITION)
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val scope = lifecycle.coroutineScope

        val listenCode = uri.getQueryParameter("code")
            ?: uri.getQueryParameter("room")
            ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" ||
            uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            listenTogetherManager.joinRoom(
                listenCode,
                dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" },
            )
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { id ->
                if (id.startsWith("OLAK5uy_")) {
                    scope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(id).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) { navController.navigate("album/$browseId") }
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$id")
                }
            }
            "browse"       -> uri.lastPathSegment?.let { navController.navigate("album/$it") }
            "channel", "c" -> uri.lastPathSegment?.let { navController.navigate("artist/$it") }
            "search"       -> uri.getQueryParameter("q")?.let {
                navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
            }
            else -> {
                val videoId = when {
                    path == "watch"        -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else                   -> null
                }
                val playlistId = uri.getQueryParameter("list")
                if (videoId != null) {
                    scope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(videoId), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                        queue.firstOrNull()?.toMediaMetadata(),
                                    )
                                )
                            }
                        }.onFailure { reportException(it) }
                    }
                } else if (playlistId != null) {
                    scope.launch(Dispatchers.IO) {
                        YouTube.queue(null, playlistId).onSuccess { queue ->
                            val first = queue.firstOrNull()
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = first?.id, playlistId = playlistId),
                                        first?.toMediaMetadata(),
                                    )
                                )
                            }
                        }.onFailure { reportException(it) }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars     = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase               = staticCompositionLocalOf<MusicDatabase>          { error("No database provided") }
val LocalPlayerConnection       = staticCompositionLocalOf<PlayerConnection?>       { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets>                { error("No WindowInsets provided") }
val LocalDownloadUtil           = staticCompositionLocalOf<DownloadUtil>            { error("No DownloadUtil provided") }
val LocalSyncUtils              = staticCompositionLocalOf<SyncUtils>               { error("No SyncUtils provided") }
val LocalListenTogetherManager  = staticCompositionLocalOf<com.dare.music.listentogether.ListenTogetherManager?> { null }
val LocalChangelogState         = staticCompositionLocalOf<MutableState<Boolean>>   { error("No LocalChangelogState provided") }
val LocalIsPlayerExpanded       = compositionLocalOf { false }
val LocalGlassBackdrop          = staticCompositionLocalOf<Backdrop?>               { null }
