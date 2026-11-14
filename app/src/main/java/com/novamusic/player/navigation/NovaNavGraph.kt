package com.novamusic.player.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.novamusic.player.data.model.Song
import com.novamusic.player.ui.NovaViewModelFactory
import com.novamusic.player.ui.components.LiquidGlassBackdrop
import com.novamusic.player.ui.components.LiquidGlassNavTab
import com.novamusic.player.ui.components.LiquidGlassNavigationBar
import com.novamusic.player.ui.components.MiniPlayer
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
import com.novamusic.player.ui.settings.SettingsScreen
import com.novamusic.player.ui.settings.SettingsViewModel

@Composable
fun NovaMainAppShell(
    container: AppContainer,
    openNowPlayingTrigger: Int = 0
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
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val eqState by equalizerViewModel.eqState.collectAsStateWithLifecycle()
    var showNowPlaying by remember { mutableStateOf(openNowPlayingTrigger > 0) }
    var lyricsTargetSong by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(openNowPlayingTrigger) {
        if (openNowPlayingTrigger > 0) {
            showNowPlaying = true
        }
    }

    BackHandler(enabled = lyricsTargetSong != null) {
        lyricsTargetSong = null
    }
    BackHandler(enabled = showNowPlaying && lyricsTargetSong == null) {
        showNowPlaying = false
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NovaDestinations.HOME

    val bottomTabs = remember {
        listOf(
            LiquidGlassNavTab(NovaDestinations.HOME, "Home", Icons.Rounded.Home),
            LiquidGlassNavTab(NovaDestinations.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
            LiquidGlassNavTab(NovaDestinations.PLAYLISTS, "Playlists", Icons.Rounded.QueueMusic),
            LiquidGlassNavTab(NovaDestinations.EQUALIZER, "EQ", Icons.Rounded.Equalizer),
            LiquidGlassNavTab(NovaDestinations.SETTINGS, "Settings", Icons.Rounded.Settings)
        )
    }

    val miniPlayerHeight = if (playerState.currentSong != null) 88.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Live blurred album artwork + ambient liquid refraction backdrop behind all screens
        LiquidGlassBackdrop(
            artworkUri = playerState.currentSong?.artworkUri,
            isPlaying = playerState.isPlaying,
            intensity = 0.9f
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onPrevious = playerViewModel::skipToPrevious,
                        onNext = playerViewModel::skipToNext,
                        onOpenNowPlaying = { showNowPlaying = true }
                    )

                    LiquidGlassNavigationBar(
                        tabs = bottomTabs,
                        currentRoute = currentRoute,
                        isEqualizerActive = eqState.isEnabled,
                        onSelectTab = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NovaDestinations.HOME,
                enterTransition = {
                    fadeIn(tween(240)) + scaleIn(initialScale = 0.985f, animationSpec = tween(240))
                },
                exitTransition = { fadeOut(tween(180)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(NovaDestinations.HOME) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        isEqualizerActive = eqState.isEnabled,
                        onOpenSearch = { navController.navigate(NovaDestinations.LIBRARY_SEARCH) },
                        onOpenEqualizer = { navController.navigate(NovaDestinations.EQUALIZER) },
                        onOpenSettings = { navController.navigate(NovaDestinations.SETTINGS) },
                        onOpenNowPlaying = { showNowPlaying = true },
                        onOpenAlbum = { albumId -> navController.navigate(NovaDestinations.albumDetail(albumId)) },
                        onOpenArtist = { artist -> navController.navigate(NovaDestinations.artistDetail(artist)) },
                        onOpenPlaylist = { pid -> navController.navigate(NovaDestinations.playlistDetail(pid)) },
                        onOpenLibrary = { navController.navigate(NovaDestinations.LIBRARY) },
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
                        onBack = null,
                        bottomContentPadding = miniPlayerHeight
                    )
                }

                composable(NovaDestinations.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = null,
                        onOpenEqualizer = { navController.navigate(NovaDestinations.EQUALIZER) },
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

        // Full-Screen Liquid Glass Now Playing Overlay
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(
                animationSpec = tween(340),
                initialOffsetY = { it }
            ) + fadeIn(tween(260)),
            exit = slideOutVertically(
                animationSpec = tween(280),
                targetOffsetY = { it }
            ) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NowPlayingScreen(
                    viewModel = playerViewModel,
                    onCollapse = { showNowPlaying = false },
                    onOpenEqualizer = {
                        showNowPlaying = false
                        navController.navigate(NovaDestinations.EQUALIZER)
                    },
                    onOpenLyrics = { song -> lyricsTargetSong = song }
                )
            }
        }

        // Full-Screen Liquid Glass Lyrics Overlay
        AnimatedVisibility(
            visible = lyricsTargetSong != null,
            enter = slideInVertically(
                animationSpec = tween(320),
                initialOffsetY = { it }
            ) + fadeIn(tween(240)),
            exit = slideOutVertically(
                animationSpec = tween(260),
                targetOffsetY = { it }
            ) + fadeOut(tween(180))
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
    }
}
