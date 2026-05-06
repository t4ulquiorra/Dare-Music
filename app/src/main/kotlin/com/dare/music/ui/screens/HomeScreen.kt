/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * HomeScreen — visual architecture ported from Xevrae/SimpMusic
 * Adapted for Dare's Hilt + Android resources + existing ViewModel
 */
package com.dare.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
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
import com.dare.music.R
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

// ── Palette ───────────────────────────────────────────────────────────────────
private val DareBackground    = Color(0xFF000000)
private val DareCard          = Color(0xFF1C1C1E)
private val DareAccent        = Color(0xFF7C3AED)
private val DareTextPrimary   = Color(0xFFFFFFFF)
private val DareTextSecondary = Color(0xFF9CA3AF)

// ── Mood chips ────────────────────────────────────────────────────────────────
private val moodChips = listOf(
    "All", "Relax", "Sleep", "Energize", "Sad",
    "Romance", "Feel Good", "Workout", "Party", "Commute", "Focus",
)

// ── isScrollingUp extension ───────────────────────────────────────────────────
@Composable
private fun androidx.compose.foundation.lazy.LazyListState.isScrollingUp(): State<Boolean> {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                (previousIndex > firstVisibleItemIndex).also { previousIndex = firstVisibleItemIndex }
            } else {
                (previousOffset >= firstVisibleItemScrollOffset).also { previousOffset = firstVisibleItemScrollOffset }
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
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
    val context          = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    // Account dialog
    var showAccountDialog by remember { mutableStateOf(false) }
    if (showAccountDialog) {
        AccountSettingsDialog(
            navController     = navController,
            onDismiss         = { showAccountDialog = false },
            latestVersionName = com.dare.music.BuildConfig.VERSION_NAME,
        )
    }

    // Playback state
    val isPlaying     by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // Data
    val quickPicks           by viewModel.quickPicks.collectAsState()
    val relatedAlbums        by viewModel.relatedAlbums.collectAsState()
    val similarArtists       by viewModel.similarArtists.collectAsState()
    val recommendedPlaylists by viewModel.recommendedPlaylists.collectAsState()
    val dailyDiscover        by viewModel.dailyDiscover.collectAsState()

    // Scroll
    val scrollState  = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val chipRowState = rememberScrollState()

    // Chip selection
    var selectedChip by rememberSaveable { mutableStateOf("All") }

    // TopAppBar height — used to push content below it
    var topBarHeightPx by remember { mutableIntStateOf(0) }

    // Dominant color from first quickPick thumbnail → angled gradient header
    var headerColor by remember { mutableStateOf(DareBackground) }
    val animatedHeaderColor by animateColorAsState(headerColor, tween(600), label = "headerColor")

    LaunchedEffect(quickPicks) {
        val url = quickPicks?.firstOrNull()?.thumbnailUrl ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val req    = ImageRequest.Builder(context).data(url).size(80, 80).allowHardware(false).build()
            val bitmap = runCatching { context.imageLoader.execute(req) }.getOrNull()?.image?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                val raw     = palette.getDominantColor(0xFF000000.toInt())
                val r       = ((raw shr 16 and 0xFF) * 0.25f) / 255f
                val g       = ((raw shr  8 and 0xFF) * 0.25f) / 255f
                val b       = ((raw        and 0xFF) * 0.25f) / 255f
                withContext(Dispatchers.Main) { headerColor = Color(r, g, b, 1f) }
            }
        }
    }

    // ytGridItem lambda
    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item           = item,
            isActive       = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying      = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem     -> {
                            if (!isListenTogetherGuest) playerConnection.playQueue(
                                YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata())
                            )
                        }
                        is AlbumItem    -> navController.navigate("album/${item.id}")
                        is ArtistItem   -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        is PodcastItem  -> navController.navigate("online_podcast/${item.id}")
                        is EpisodeItem  -> {
                            if (!isListenTogetherGuest) playerConnection.playQueue(
                                ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem()))
                            )
                        }
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem     -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                            is AlbumItem    -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                            is ArtistItem   -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                            else            -> {}
                        }
                    }
                },
            ),
        )
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box {

        LazyColumn(
            state           = scrollState,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding  = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
        ) {

            // ── Gradient header + Quick Picks ──────────────────────────────
            item {
                Box {
                    // Angled dominant-color gradient — fades to black
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to animatedHeaderColor,
                                        0.6f to animatedHeaderColor.copy(alpha = 0.6f),
                                        1.0f to DareBackground,
                                    )
                                )
                            ),
                    )
                    // Extra fade to pure black at bottom of header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xBB000000), DareBackground)
                                )
                            ),
                    )

                    // Content above gradient
                    quickPicks?.takeIf { it.isNotEmpty() }?.let { songs ->
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(
                                    top = with(LocalDensity.current) { topBarHeightPx.toDp() } + 12.dp,
                                ),
                        ) {
                            Text(
                                text  = "Let's start with a radio",
                                color = DareTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text       = "Quick Picks",
                                color      = DareTextPrimary,
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            )
                            // 4-row horizontal grid with snap — Xevrae's exact pattern
                            val gridState = rememberLazyGridState()
                            LazyHorizontalGrid(
                                rows                 = GridCells.Fixed(4),
                                modifier             = Modifier.height(256.dp),
                                state                = gridState,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement  = Arrangement.spacedBy(6.dp),
                            ) {
                                items(songs) { song ->
                                    QuickPickRow(
                                        song                  = song,
                                        isActive              = song.id == mediaMetadata?.id,
                                        isPlaying             = isPlaying && song.id == mediaMetadata?.id,
                                        onClick = {
                                            if (!isListenTogetherGuest) {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong  = song,
                                                    navController = navController,
                                                    onDismiss     = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Made for You ──────────────────────────────────────────────
            if (recommendedPlaylists.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(subtitle = "Curated for you", title = "Made for You")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = recommendedPlaylists, key = { it.id }) {
                                Box(Modifier.width(160.dp)) { ytGridItem(it) }
                            }
                        }
                    }
                }
            }

            // ── New Releases ───────────────────────────────────────────────
            if (relatedAlbums.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(subtitle = "Fresh drops", title = "New Releases")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = relatedAlbums, key = { it.id }) {
                                Box(Modifier.width(160.dp)) { ytGridItem(it) }
                            }
                        }
                    }
                }
            }

            // ── Trending Now ───────────────────────────────────────────────
            dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discover ->
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(subtitle = "What's hot right now", title = "Trending Now")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = discover, key = { it.recommendation.id }) {
                                Box(Modifier.width(160.dp)) { ytGridItem(it.recommendation) }
                            }
                        }
                    }
                }
            }

            // ── Artists You Might Like ─────────────────────────────────────
            if (similarArtists.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(subtitle = "Based on your taste", title = "Artists You Might Like")
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = similarArtists, key = { it.id }) {
                                Box(Modifier.width(120.dp)) { ytGridItem(it) }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        // ── Floating TopAppBar overlay ─────────────────────────────────────
        // Transparent at top, blurred-dark on scroll — exactly Xevrae's pattern
        AnimatedContent(
            targetState = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(250))) },
            label = "topBarBg",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { topBarHeightPx = it.size.height },
        ) { atTop ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (atTop) Color.Transparent
                        else Color.Black.copy(alpha = 0.75f)
                    ),
            ) {
                // TopAppBar: hides when scrolling DOWN, reappears scrolling UP
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    HomeTopBar(
                        onAccountClick = { showAccountDialog = true },
                        navController  = navController,
                    )
                }
                // Status bar spacer when TopAppBar is hidden
                AnimatedVisibility(
                    visible = !isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    )
                }
                // Chip row — always visible, scrollable
                Row(
                    modifier = Modifier
                        .horizontalScroll(chipRowState)
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    moodChips.forEach { chip ->
                        val isSelected = chip == selectedChip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) DareAccent
                                    else DareCard.copy(alpha = 0.85f)
                                )
                                .clickable { selectedChip = chip }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text       = chip,
                                color      = DareTextPrimary,
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    onAccountClick: () -> Unit,
    navController: NavController,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 6..12  -> "Good morning"
        in 13..17 -> "Good afternoon"
        in 18..23 -> "Good evening"
        else      -> "Good night"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text       = "Dare",
                style      = MaterialTheme.typography.titleMedium,
                color      = DareTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = DareTextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { navController.navigate("history") }) {
                Icon(painterResource(R.drawable.history), contentDescription = null, tint = DareTextSecondary)
            }
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(painterResource(R.drawable.settings), contentDescription = null, tint = DareTextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DareCard)
                    .clickable(onClick = onAccountClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = null,
                    tint     = DareTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(subtitle: String, title: String) {
    Text(
        text  = subtitle,
        color = DareTextSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text       = title,
        color      = DareTextPrimary,
        style      = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickRow(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DareCard.copy(alpha = 0.75f))
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
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = song.title,
                color      = if (isActive) DareAccent else DareTextPrimary,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = song.artists.joinToString { it.name },
                color    = DareTextSecondary,
                style    = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isActive) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint     = DareAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
