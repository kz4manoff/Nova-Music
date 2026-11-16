package com.novamusic.player.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novamusic.player.AppContainer
import com.novamusic.player.data.model.PlayerAnimationStyle
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.NovaViewModelFactory
import com.novamusic.player.ui.components.AudioDeviceEventPopup
import com.novamusic.player.ui.components.AudioOutputSwitcherSheet
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassMiniPlayer
import com.novamusic.player.ui.components.LiquidGlassNavTab
import com.novamusic.player.ui.components.LiquidGlassTabBar
import com.novamusic.player.ui.components.LocalBlurIntensity
import com.novamusic.player.ui.components.LocalHapticEnabled
import com.novamusic.player.ui.components.LocalNavBarAppearance
import com.novamusic.player.ui.components.LocalPlayerAnimationStyle
import com.novamusic.player.ui.components.LocalReducedMotion
import com.novamusic.player.ui.components.rememberReducedMotion
import kotlin.math.roundToInt
import com.novamusic.player.ui.equalizer.EqualizerScreen
import com.novamusic.player.ui.equalizer.EqualizerViewModel
import com.novamusic.player.ui.home.HomeScreen
import com.novamusic.player.ui.home.HomeViewModel
import com.novamusic.player.ui.library.CollectionDetailScreen
import com.novamusic.player.ui.library.LibraryScreen
import com.novamusic.player.ui.library.LibraryViewModel
import com.novamusic.player.ui.player.LyricsScreen
import com.novamusic.player.ui.player.NowPlayingScreen
import com.novamusic.player.ui.player.PlayerViewModel
import com.novamusic.player.ui.playlists.PlaylistDetailScreen
import com.novamusic.player.ui.playlists.PlaylistsScreen
import com.novamusic.player.ui.playlists.PlaylistsViewModel
import com.novamusic.player.ui.search.SearchScreen
import com.novamusic.player.ui.settings.AboutScreen
import com.novamusic.player.ui.settings.AppUpdatesSettingsPage
import com.novamusic.player.ui.settings.AppearanceSettingsPage
import com.novamusic.player.ui.settings.AudioEqualizerSettingsPage
import com.novamusic.player.ui.settings.DownloadsSettingsPage
import com.novamusic.player.ui.settings.HeadphonesSettingsPage
import com.novamusic.player.ui.settings.LibraryManagementSettingsPage
import com.novamusic.player.ui.settings.NotificationsSettingsPage
import com.novamusic.player.ui.settings.PlaybackSettingsPage
import com.novamusic.player.ui.settings.PrivacySettingsPage
import com.novamusic.player.ui.settings.SettingsScreen
import com.novamusic.player.ui.settings.SettingsViewModel
import com.novamusic.player.ui.settings.StorageSettingsPage
import com.novamusic.player.ui.theme.LocalArtworkPalette
import com.novamusic.player.ui.theme.rememberArtworkPalette
import com.novamusic.player.update.UpdateUiState

@Composable
fun NovaMainAppShell(
    container: AppContainer,
    openNowPlayingTrigger: Int = 0,
    openUpdatesTrigger: Int = 0
) {
    val navController = rememberNavController()
    val factory = remember(container) { NovaViewModelFactory(container) }

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val playerViewModel: PlayerViewModel = viewModel(factory = factory)
    val equalizerViewModel: EqualizerViewModel = viewModel(factory = factory)
    val playlistsViewModel: PlaylistsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val audioRoutingState by playerViewModel.audioRoutingState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by settingsViewModel.appSettings.collectAsStateWithLifecycle()
    val updateState by container.updateManager.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNowPlaying by remember { mutableStateOf(openNowPlayingTrigger > 0) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }
    var lyricsTargetSong by remember { mutableStateOf<Song?>(null) }
    var nowPlayingCollapseProgress by remember { mutableFloatStateOf(0f) }
    var miniPlayerDismissed by rememberSaveable { mutableStateOf(false) }
    var lastObservedSongId by rememberSaveable { mutableStateOf<Long?>(null) }

    val pendingUpdateVersion = when (val state = updateState) {
        is UpdateUiState.Available -> state.config.latestVersion
        is UpdateUiState.ReadyToInstall -> state.config.latestVersion
        else -> null
    }

    val openUpdatesDestination: () -> Unit = {
        showNowPlaying = false
        lyricsTargetSong = null
        showAudioOutputSheet = false
        val routeNow = navController.currentBackStackEntry?.destination?.route
        if (routeNow != NovaDestinations.SETTINGS_UPDATES) {
            if (routeNow != NovaDestinations.SETTINGS) {
                navController.navigate(NovaDestinations.SETTINGS) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            navController.navigate(NovaDestinations.SETTINGS_UPDATES) {
                launchSingleTop = true
            }
        }
    }

    val artworkPalette = rememberArtworkPalette(
        artworkUri = playerState.currentSong?.artworkUri,
        fallbackAccent = MaterialTheme.colorScheme.primary
    )

    LaunchedEffect(openNowPlayingTrigger) {
        if (openNowPlayingTrigger > 0) {
            showNowPlaying = true
        }
    }

    LaunchedEffect(openUpdatesTrigger) {
        if (openUpdatesTrigger > 0) {
            openUpdatesDestination()
        }
    }

    LaunchedEffect(showNowPlaying) {
        if (!showNowPlaying) {
            nowPlayingCollapseProgress = 0f
        }
    }

    LaunchedEffect(playerState.currentSong?.id, playerState.isPlaying) {
        val currentId = playerState.currentSong?.id
        if (currentId == null) {
            miniPlayerDismissed = false
            lastObservedSongId = null
        } else {
            if (currentId != lastObservedSongId || playerState.isPlaying) {
                miniPlayerDismissed = false
            }
            lastObservedSongId = currentId
        }
    }

    BackHandler(enabled = lyricsTargetSong != null) {
        lyricsTargetSong = null
    }
    BackHandler(enabled = showNowPlaying && lyricsTargetSong == null) {
        nowPlayingCollapseProgress = 0f
        showNowPlaying = false
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NovaDestinations.HOME

    // Required 5 Floating Liquid Glass Tabs: Home, Library, Search, Playlists, Settings
    val hasPendingUpdate = pendingUpdateVersion != null
    val bottomTabs = remember(hasPendingUpdate) {
        listOf(
            LiquidGlassNavTab(NovaDestinations.HOME, "Home", Icons.Rounded.Home),
            LiquidGlassNavTab(NovaDestinations.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
            LiquidGlassNavTab(NovaDestinations.SEARCH, "Search", Icons.Rounded.Search),
            LiquidGlassNavTab(NovaDestinations.PLAYLISTS, "Playlists", Icons.Rounded.QueueMusic),
            LiquidGlassNavTab(
                NovaDestinations.SETTINGS,
                "Settings",
                Icons.Rounded.Settings,
                hasBadge = hasPendingUpdate
            )
        )
    }

    val navigateTab: (String) -> Unit = { route ->
        if (route == NovaDestinations.SETTINGS &&
            (currentRoute.startsWith("settings/") || currentRoute == NovaDestinations.ABOUT)
        ) {
            navController.popBackStack(NovaDestinations.SETTINGS, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val isMiniPlayerVisible = playerState.currentSong != null && !miniPlayerDismissed
    val miniPlayerHeight = if (isMiniPlayerVisible) 86.dp else 0.dp
    val reducedMotion = rememberReducedMotion() || !appSettings.interfaceAnimations
    val overlayOpen = showNowPlaying || lyricsTargetSong != null
    val activeCollapseFraction = if (showNowPlaying && lyricsTargetSong == null) {
        nowPlayingCollapseProgress.coerceIn(0f, 1f)
    } else {
        0f
    }
    val bgScale by animateFloatAsState(
        targetValue = if (overlayOpen && !reducedMotion) {
            0.945f + (1f - 0.945f) * activeCollapseFraction
        } else {
            1f
        },
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shell_bg_scale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (overlayOpen) {
            0.65f + (1f - 0.65f) * activeCollapseFraction
        } else {
            1f
        },
        animationSpec = if (reducedMotion) snap() else tween(260, easing = FastOutSlowInEasing),
        label = "shell_bg_alpha"
    )

    CompositionLocalProvider(
        LocalArtworkPalette provides artworkPalette,
        LocalReducedMotion provides reducedMotion,
        LocalHapticEnabled provides appSettings.hapticFeedback,
        LocalBlurIntensity provides appSettings.blurIntensity,
        LocalPlayerAnimationStyle provides appSettings.playerAnimationStyle,
        LocalNavBarAppearance provides appSettings.navBarAppearance
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live blurred album artwork + extracted color ambient light scatter behind all screens
            LiquidGlassBackdrop(
                artworkUri = playerState.currentSong?.artworkUri,
                isPlaying = playerState.isPlaying,
                intensity = (0.92f * appSettings.blurIntensity).coerceIn(0.25f, 1.55f)
            )

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = bgScale
                        scaleY = bgScale
                        alpha = bgAlpha
                    }
                    .statusBarsPadding(),
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.84f)
                                    )
                                )
                            )
                            .navigationBarsPadding()
                    ) {
                        LiquidGlassMiniPlayer(
                            playerState = if (miniPlayerDismissed) {
                                playerState.copy(currentSong = null)
                            } else {
                                playerState
                            },
                            onTogglePlayPause = playerViewModel::togglePlayPause,
                            onPrevious = playerViewModel::skipToPrevious,
                            onNext = playerViewModel::skipToNext,
                            onOpenNowPlaying = {
                                miniPlayerDismissed = false
                                showNowPlaying = true
                            },
                            onDismissMiniPlayer = {
                                miniPlayerDismissed = true
                                playerViewModel.pausePlayback()
                            },
                            activeOutputDevice = audioRoutingState.activeDevice,
                            onOpenAudioOutput = { showAudioOutputSheet = true }
                        )

                        LiquidGlassTabBar(
                            tabs = bottomTabs,
                            currentRoute = currentRoute,
                            onSelectTab = { tab -> navigateTab(tab.route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = NovaDestinations.HOME,
                    enterTransition = {
                        if (reducedMotion) {
                            fadeIn(tween(90))
                        } else {
                            fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
                                    initialOffsetX = { (it * 0.05f).roundToInt() }
                                ) +
                                scaleIn(
                                    initialScale = 0.968f,
                                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
                                )
                        }
                    },
                    exitTransition = {
                        if (reducedMotion) {
                            fadeOut(tween(90))
                        } else {
                            fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(
                                    animationSpec = tween(210, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -(it * 0.03f).roundToInt() }
                                ) +
                                scaleOut(targetScale = 0.985f, animationSpec = tween(200))
                        }
                    },
                    popEnterTransition = {
                        if (reducedMotion) {
                            fadeIn(tween(90))
                        } else {
                            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
                                    initialOffsetX = { -(it * 0.05f).roundToInt() }
                                ) +
                                scaleIn(
                                    initialScale = 0.975f,
                                    animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
                                )
                        }
                    },
                    popExitTransition = {
                        if (reducedMotion) {
                            fadeOut(tween(90))
                        } else {
                            fadeOut(tween(190, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { (it * 0.05f).roundToInt() }
                                ) +
                                scaleOut(targetScale = 0.965f, animationSpec = tween(210))
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    composable(NovaDestinations.HOME) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenSearch = { navigateTab(NovaDestinations.SEARCH) },
                            onOpenNowPlaying = { showNowPlaying = true },
                            onOpenAlbum = { albumId -> navController.navigate(NovaDestinations.albumDetail(albumId)) },
                            onOpenArtist = { artist -> navController.navigate(NovaDestinations.artistDetail(artist)) },
                            onOpenPlaylist = { pid -> navController.navigate(NovaDestinations.playlistDetail(pid)) },
                            onOpenLibrary = { navigateTab(NovaDestinations.LIBRARY) },
                            availableUpdateVersion = pendingUpdateVersion,
                            onOpenUpdates = openUpdatesDestination,
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = "library?search={search}",
                        arguments = listOf(navArgument("search") {
                            type = NavType.BoolType
                            defaultValue = false
                        })
                    ) { entry ->
                        val focusSearch = entry.arguments?.getBoolean("search") ?: false
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            initialFocusSearch = focusSearch,
                            onOpenAlbum = { albumId -> navController.navigate(NovaDestinations.albumDetail(albumId)) },
                            onOpenArtist = { artist -> navController.navigate(NovaDestinations.artistDetail(artist)) },
                            onOpenFolder = { path -> navController.navigate(NovaDestinations.folderDetail(path)) },
                            onOpenGenre = { genre -> navController.navigate(NovaDestinations.genreDetail(genre)) },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SEARCH) {
                        SearchScreen(
                            viewModel = libraryViewModel,
                            onOpenAlbum = { albumId -> navController.navigate(NovaDestinations.albumDetail(albumId)) },
                            onOpenArtist = { artist -> navController.navigate(NovaDestinations.artistDetail(artist)) },
                            onOpenGenre = { genre -> navController.navigate(NovaDestinations.genreDetail(genre)) },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.PLAYLISTS) {
                        PlaylistsScreen(
                            viewModel = playlistsViewModel,
                            onOpenPlaylist = { pid -> navController.navigate(NovaDestinations.playlistDetail(pid)) },
                            onOpenFavorites = { navController.navigate(NovaDestinations.FAVORITES_DETAIL) },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.EQUALIZER) {
                        EqualizerScreen(
                            viewModel = equalizerViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = null,
                            bottomContentPadding = miniPlayerHeight,
                            onOpenAppearance = { navController.navigate(NovaDestinations.SETTINGS_APPEARANCE) },
                            onOpenPlayback = { navController.navigate(NovaDestinations.SETTINGS_PLAYBACK) },
                            onOpenAudioEqualizer = { navController.navigate(NovaDestinations.SETTINGS_EQUALIZER) },
                            onOpenHeadphones = { navController.navigate(NovaDestinations.SETTINGS_HEADPHONES) },
                            onOpenNotifications = { navController.navigate(NovaDestinations.SETTINGS_NOTIFICATIONS) },
                            onOpenLibraryManagement = { navController.navigate(NovaDestinations.SETTINGS_LIBRARY) },
                            onOpenDownloads = { navController.navigate(NovaDestinations.SETTINGS_DOWNLOADS) },
                            onOpenStorage = { navController.navigate(NovaDestinations.SETTINGS_STORAGE) },
                            onOpenAppUpdates = { navController.navigate(NovaDestinations.SETTINGS_UPDATES) },
                            onOpenPrivacy = { navController.navigate(NovaDestinations.SETTINGS_PRIVACY) },
                            onOpenAbout = { navController.navigate(NovaDestinations.SETTINGS_ABOUT) }
                        )
                    }

                    composable(NovaDestinations.SETTINGS_APPEARANCE) {
                        AppearanceSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_PLAYBACK) {
                        PlaybackSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_EQUALIZER) {
                        AudioEqualizerSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenFullEqualizer = { navController.navigate(NovaDestinations.EQUALIZER) },
                            onOpenAudioOutput = { showAudioOutputSheet = true },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_HEADPHONES) {
                        HeadphonesSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenAudioOutput = { showAudioOutputSheet = true },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_NOTIFICATIONS) {
                        NotificationsSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_LIBRARY) {
                        LibraryManagementSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_DOWNLOADS) {
                        DownloadsSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_STORAGE) {
                        StorageSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_UPDATES) {
                        AppUpdatesSettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_PRIVACY) {
                        PrivacySettingsPage(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.SETTINGS_ABOUT) {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.ABOUT) {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = NovaDestinations.ALBUM_DETAIL,
                        arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                    ) { entry ->
                        val albumId = entry.arguments?.getLong("albumId") ?: 0L
                        val songs = remember(libraryState.songs, albumId) {
                            libraryViewModel.songsForAlbum(albumId)
                        }
                        val albumName = songs.firstOrNull()?.album ?: "Album"
                        val artistName = songs.firstOrNull()?.artist ?: ""
                        CollectionDetailScreen(
                            title = albumName,
                            subtitle = artistName,
                            artworkUri = songs.firstOrNull()?.artworkUri,
                            songs = songs,
                            viewModel = libraryViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = NovaDestinations.ARTIST_DETAIL,
                        arguments = listOf(navArgument("artistName") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { entry ->
                        val artistName = Uri.decode(entry.arguments?.getString("artistName").orEmpty())
                        val songs = remember(libraryState.songs, artistName) {
                            libraryViewModel.songsForArtist(artistName)
                        }
                        CollectionDetailScreen(
                            title = artistName,
                            subtitle = "Artist",
                            artworkUri = songs.firstOrNull()?.artworkUri,
                            songs = songs,
                            viewModel = libraryViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = NovaDestinations.FOLDER_DETAIL,
                        arguments = listOf(navArgument("folderPath") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { entry ->
                        val folderPath = Uri.decode(entry.arguments?.getString("folderPath").orEmpty())
                        val songs = remember(libraryState.songs, folderPath) {
                            libraryViewModel.songsForFolder(folderPath)
                        }
                        val folderName = songs.firstOrNull()?.folderName ?: "Folder"
                        CollectionDetailScreen(
                            title = folderName,
                            subtitle = folderPath,
                            artworkUri = songs.firstOrNull()?.artworkUri,
                            songs = songs,
                            viewModel = libraryViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = NovaDestinations.GENRE_DETAIL,
                        arguments = listOf(navArgument("genreName") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { entry ->
                        val genreName = Uri.decode(entry.arguments?.getString("genreName").orEmpty())
                        val songs = remember(libraryState.songs, genreName) {
                            libraryViewModel.songsForGenre(genreName)
                        }
                        CollectionDetailScreen(
                            title = genreName,
                            subtitle = "Genre",
                            artworkUri = songs.firstOrNull()?.artworkUri,
                            songs = songs,
                            viewModel = libraryViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenLyricsForSong = { song -> lyricsTargetSong = song },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(
                        route = NovaDestinations.PLAYLIST_DETAIL,
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { entry ->
                        val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            isFavoritesMode = false,
                            viewModel = playlistsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }

                    composable(NovaDestinations.FAVORITES_DETAIL) {
                        PlaylistDetailScreen(
                            playlistId = -1L,
                            isFavoritesMode = true,
                            viewModel = playlistsViewModel,
                            onBack = { navController.popBackStack() },
                            bottomContentPadding = miniPlayerHeight
                        )
                    }
                }
            }

            // Full-Screen Fluid Morph Liquid Glass Now Playing Sheet Overlay expanding from Mini Player artwork origin
            AnimatedVisibility(
                visible = showNowPlaying,
                enter = when {
                    reducedMotion -> fadeIn(tween(110))
                    appSettings.playerAnimationStyle == PlayerAnimationStyle.MINIMAL -> fadeIn(tween(180))
                    else -> slideInVertically(
                        animationSpec = spring<IntOffset>(
                            dampingRatio = if (appSettings.playerAnimationStyle == PlayerAnimationStyle.FLUID) 0.82f else 0.90f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { (it * 0.34f).roundToInt() }
                    ) + scaleIn(
                        initialScale = 0.86f,
                        transformOrigin = TransformOrigin(0.20f, 0.90f),
                        animationSpec = spring(
                            dampingRatio = if (appSettings.playerAnimationStyle == PlayerAnimationStyle.FLUID) 0.82f else 0.90f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(240, easing = FastOutSlowInEasing))
                },
                exit = when {
                    reducedMotion -> fadeOut(tween(100))
                    appSettings.playerAnimationStyle == PlayerAnimationStyle.MINIMAL -> fadeOut(tween(150))
                    else -> slideOutVertically(
                        animationSpec = spring<IntOffset>(
                            dampingRatio = 0.92f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        targetOffsetY = { (it * 0.34f).roundToInt() }
                    ) + scaleOut(
                        targetScale = 0.86f,
                        transformOrigin = TransformOrigin(0.20f, 0.90f),
                        animationSpec = tween(210, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(190))
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    NowPlayingScreen(
                        viewModel = playerViewModel,
                        onCollapse = {
                            nowPlayingCollapseProgress = 0f
                            showNowPlaying = false
                        },
                        onOpenEqualizer = {
                            nowPlayingCollapseProgress = 0f
                            showNowPlaying = false
                            navController.navigate(NovaDestinations.EQUALIZER)
                        },
                        onOpenLyrics = { song -> lyricsTargetSong = song },
                        onOpenAlbum = { albumId ->
                            nowPlayingCollapseProgress = 0f
                            showNowPlaying = false
                            navController.navigate(NovaDestinations.albumDetail(albumId))
                        },
                        onOpenArtist = { artistName ->
                            nowPlayingCollapseProgress = 0f
                            showNowPlaying = false
                            navController.navigate(NovaDestinations.artistDetail(artistName))
                        },
                        onCollapseProgress = { progress -> nowPlayingCollapseProgress = progress }
                    )
                }
            }

            // Full-Screen Liquid Glass Lyrics Overlay
            AnimatedVisibility(
                visible = lyricsTargetSong != null,
                enter = if (reducedMotion) {
                    fadeIn(tween(110))
                } else {
                    slideInVertically(
                        animationSpec = spring<IntOffset>(
                            dampingRatio = 0.84f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { it }
                    ) + fadeIn(tween(240))
                },
                exit = if (reducedMotion) {
                    fadeOut(tween(100))
                } else {
                    slideOutVertically(
                        animationSpec = spring<IntOffset>(
                            dampingRatio = 0.90f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        targetOffsetY = { it }
                    ) + fadeOut(tween(190))
                }
            ) {
                lyricsTargetSong?.let { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        LyricsScreen(
                            song = song,
                            viewModel = playerViewModel,
                            onBack = { lyricsTargetSong = null }
                        )
                    }
                }
            }

            // Real-time floating Liquid Glass audio device connection / disconnection popup
            AudioDeviceEventPopup(
                banner = audioRoutingState.activeBanner,
                onDismiss = playerViewModel::dismissDeviceBanner,
                onOpenSwitcher = { showAudioOutputSheet = true }
            )

            if (showAudioOutputSheet) {
                AudioOutputSwitcherSheet(
                    routingState = audioRoutingState,
                    isPlaying = playerState.isPlaying,
                    onSelectDevice = playerViewModel::selectAudioOutput,
                    onRefreshPermissions = playerViewModel::refreshAudioDevices,
                    onOpenSystemOutput = { playerViewModel.openSystemSoundOutputPanel(context) },
                    onOpenBluetoothSettings = { playerViewModel.openBluetoothSettings(context) },
                    onDismiss = { showAudioOutputSheet = false }
                )
            }
        }
    }
}
