/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.dare.innertube.models.AlbumItem
import com.dare.innertube.models.ArtistItem
import com.dare.innertube.models.EpisodeItem
import com.dare.innertube.models.PlaylistItem
import com.dare.innertube.models.PodcastItem
import com.dare.innertube.models.SongItem
import com.dare.innertube.models.WatchEndpoint
import com.dare.innertube.models.YTItem
import com.dare.music.LocalListenTogetherManager
import com.dare.music.LocalPlayerAwareWindowInsets
import com.dare.music.LocalPlayerConnection
import com.dare.music.db.entities.Song
import com.dare.music.extensions.toMediaItem
import com.dare.music.models.toMediaMetadata
import com.dare.music.playback.queues.ListQueue
import com.dare.music.playback.queues.YouTubeQueue
import com.dare.music.ui.component.AccountSettingsDialog
import com.dare.music.ui.component.LocalMenuState
import com.dare.music.ui.component.YouTubeGridItem
import com.dare.music.ui.menu.SongMenu
import com.dare.music.ui.menu.YouTubeAlbumMenu
import com.dare.music.ui.menu.YouTubeArtistMenu
import com.dare.music.ui.menu.YouTubePlaylistMenu
import com.dare.music.ui.menu.YouTubeSongMenu
import com.dare.music.viewmodels.HomeViewModel
import java.util.Calendar

// ─── Palette ──────────────────────────────────────────────────────────────────
private val DareBackground   = Color(0xFF000000)
private val DareCard         = Color(0xFF1C1C1E)
private val DareAccent       = Color(0xFF7C3AED)
private val DareTextPrimary  = Color(0xFFFFFFFF)
private val DareTextSecondary = Color(0xFF9CA3AF)
private val DareTextHint     = Color(0xFF6B7280)

// ─── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState        = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic           = LocalHapticFeedback.current
    val scope            = rememberCoroutineScope()
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    // ── Dialog ────────────────────────────────────────────────────────────────
    var showAccountDialog by remember { mutableStateOf(false) }
    if (showAccountDialog) {
        AccountSettingsDialog(
            navController    = navController,
            onDismiss        = { showAccountDialog = false },
            latestVersionName = com.dare.music.BuildConfig.VERSION_NAME,
        )
    }

    // ── Playback state ────────────────────────────────────────────────────────
    val isPlaying     by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // ── Data flows ────────────────────────────────────────────────────────────
    val quickPicks           by viewModel.quickPicks.collectAsState()
    val relatedAlbums        by viewModel.relatedAlbums.collectAsState()
    val similarArtists       by viewModel.similarArtists.collectAsState()
    val recommendedPlaylists by viewModel.recommendedPlaylists.collectAsState()
    val dailyDiscover        by viewModel.dailyDiscover.collectAsState()

    // ── Chips ─────────────────────────────────────────────────────────────────
    var selectedChip by rememberSaveable { mutableStateOf("All") }
    val chips = remember { listOf("All", "Chill", "Hip-Hop", "Pop", "Rock", "Sad") }

    // ── Greeting ──────────────────────────────────────────────────────────────
    val greetingMain = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        else -> "Good evening,"
    }

    // ── ytGridItem lambda (handles nav + menus for YTItem types) ──────────────
    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item          = item,
            isActive      = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying     = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem -> {
                            if (!isListenTogetherGuest) {
                                playerConnection.playQueue(
                                    YouTubeQueue(
                                        item.endpoint ?: WatchEndpoint(videoId = item.id),
                                        item.toMediaMetadata(),
                                    ),
                                )
                            }
                        }
                        is AlbumItem    -> navController.navigate("album/${item.id}")
                        is ArtistItem   -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        is PodcastItem  -> navController.navigate("online_podcast/${item.id}")
                        is EpisodeItem  -> {
                            if (!isListenTogetherGuest) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = item.title,
                                        items = listOf(item.toMediaMetadata().toMediaItem()),
                                    ),
                                )
                            }
                        }
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem     -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                            is AlbumItem    -> YouTubeAlbumMenu(album = item, navController = navController, onDismiss = menuState::dismiss)
                            is ArtistItem   -> YouTubeArtistMenu(artist = item, navController = navController, onDismiss = menuState::dismiss)
                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, navController = navController, onDismiss = menuState::dismiss)
                            else            -> {}
                        }
                    }
                },
            ),
        )
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DareBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
        ) {

            // ── Greeting header ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text  = greetingMain,
                            color = DareTextPrimary,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text  = "What do you want to listen to?",
                            color = DareTextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DareCard)
                            .clickable { showAccountDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            tint = DareTextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // ── Search bar (sticky) ───────────────────────────────────────────
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DareBackground)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(DareCard)
                            .clickable { navController.navigate("search_input") }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DareTextHint,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = "Search songs, artists, albums...",
                            color = DareTextHint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ── Chips ─────────────────────────────────────────────────────────
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    items(chips) { chip ->
                        val isSelected = chip == selectedChip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) DareAccent else DareCard)
                                .clickable { selectedChip = chip }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text  = chip,
                                color = DareTextPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // ── Quick Picks ───────────────────────────────────────────────────
            quickPicks?.takeIf { it.isNotEmpty() }?.let { songs ->
                item {
                    SectionTitle(
                        title    = "Quick Picks",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        songs.take(6).chunked(2).forEach { rowSongs ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowSongs.forEach { song ->
                                    QuickPickCard(
                                        song     = song,
                                        isActive  = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying && song.id == mediaMetadata?.id,
                                        modifier  = Modifier.weight(1f),
                                        onClick = {
                                            if (!isListenTogetherGuest) {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata()),
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss    = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                }
                                if (rowSongs.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Made for You (recommendedPlaylists) ───────────────────────────
            if (recommendedPlaylists.isNotEmpty()) {
                item {
                    SectionTitle(
                        title    = "Made for you",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(items = recommendedPlaylists, key = { it.id }) { playlist ->
                            Box(Modifier.width(160.dp)) { ytGridItem(playlist) }
                        }
                    }
                }
            }

            // ── New Releases (relatedAlbums) ──────────────────────────────────
            if (relatedAlbums.isNotEmpty()) {
                item {
                    SectionTitle(
                        title    = "New releases",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(items = relatedAlbums, key = { it.id }) { album ->
                            Box(Modifier.width(160.dp)) { ytGridItem(album) }
                        }
                    }
                }
            }

            // ── Trending Now (dailyDiscover) ──────────────────────────────────
            dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discover ->
                item {
                    SectionTitle(
                        title    = "Trending now",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(items = discover, key = { it.recommendation.id }) { item ->
                            Box(Modifier.width(160.dp)) { ytGridItem(item.recommendation) }
                        }
                    }
                }
            }

            // ── Artists you might like (similarArtists) ───────────────────────
            if (similarArtists.isNotEmpty()) {
                item {
                    SectionTitle(
                        title    = "Artists you might like",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(items = similarArtists, key = { it.id }) { artist ->
                            Box(Modifier.width(120.dp)) { ytGridItem(artist) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text       = title,
        color      = Color.White,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier   = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickCard(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = song.title,
                color      = if (isActive) Color(0xFF7C3AED) else Color.White,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = song.artists.joinToString { it.name },
                color    = Color(0xFF9CA3AF),
                style    = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
