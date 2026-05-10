/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * HomeScreen — exact visual port from Xevrae/SimpMusic
 */
package com.dare.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.dare.innertube.pages.HomePage
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
import com.dare.music.ui.menu.SongMenu
import com.dare.music.ui.menu.YouTubeAlbumMenu
import com.dare.music.ui.menu.YouTubeArtistMenu
import com.dare.music.ui.menu.YouTubePlaylistMenu
import com.dare.music.ui.menu.YouTubeSongMenu
import com.dare.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// ── Sealed list entry (mirrors Xevrae's homeData approach) ───────────────────
private sealed class HomeEntry {
    data class QuickPicksEntry(val songs: List<Song>) : HomeEntry()
    data class SectionEntry(val section: HomePage.Section) : HomeEntry()
}

// ── isScrollingUp (exact Xevrae extension) ───────────────────────────────────
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
    val coroutineScope        = rememberCoroutineScope()
    val menuState             = LocalMenuState.current
    val playerConnection      = LocalPlayerConnection.current ?: return
    val haptic                = LocalHapticFeedback.current
    val context               = LocalContext.current
    val density               = LocalDensity.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    // ── ViewModel state ───────────────────────────────────────────────────────
    val accountName     by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val homePage        by viewModel.homePage.collectAsState()
    val quickPicks      by viewModel.quickPicks.collectAsState()
    val explorePage     by viewModel.explorePage.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isRefreshing    by viewModel.isRefreshing.collectAsState()
    val selectedChip    by viewModel.selectedChip.collectAsState()
    val isPlaying       by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata   by playerConnection.mediaMetadata.collectAsState()

    // ── Account dialog ────────────────────────────────────────────────────────
    var showAccountDialog by remember { mutableStateOf(false) }
    if (showAccountDialog) {
        AccountSettingsDialog(
            navController     = navController,
            onDismiss         = { showAccountDialog = false },
            latestVersionName = com.dare.music.BuildConfig.VERSION_NAME,
        )
    }

    // ── Scroll + chip row state ───────────────────────────────────────────────
    val scrollState   = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val chipRowState  = rememberScrollState()

    // ── Pull to refresh (exact Xevrae pattern) ────────────────────────────────
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) coroutineScope.launch { pullToRefreshState.animateToHidden() }
    }

    // ── TopAppBar height ──────────────────────────────────────────────────────
    var topAppBarHeightPx by remember { mutableIntStateOf(0) }

    // ── Dominant color (Xevrae's mainHomeThumbnail/rgbFactor pattern) ─────────
    var topHeaderColor by remember { mutableStateOf(Color.Black) }
    val animatedColor  by animateColorAsState(topHeaderColor, tween(500), label = "headerColor")
    val mainThumbnail  = remember(quickPicks) { quickPicks?.firstOrNull()?.thumbnailUrl }

    LaunchedEffect(mainThumbnail) {
        val url = mainThumbnail ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val req    = ImageRequest.Builder(context).data(url).size(100, 100).allowHardware(false).build()
            val bitmap = runCatching { context.imageLoader.execute(req) }.getOrNull()?.image?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                val raw     = palette.getDominantColor(0xFF000000.toInt())
                // rgbFactor(0.3f) equivalent
                val r = ((raw shr 16 and 0xFF) * 0.3f) / 255f
                val g = ((raw shr  8 and 0xFF) * 0.3f) / 255f
                val b = ((raw        and 0xFF) * 0.3f) / 255f
                withContext(Dispatchers.Main) { topHeaderColor = Color(r, g, b, 1f) }
            }
        }
    }

    // ── Pagination (exact Xevrae shouldStartPaginate pattern) ─────────────────
    val continuation = homePage?.continuation
    val homeListState = when {
        isLoading      -> ListState.LOADING
        continuation != null -> ListState.IDLE
        else           -> ListState.PAGINATION_EXHAUST
    }
    val shouldStartPaginate = remember {
        derivedStateOf {
            homeListState != ListState.PAGINATION_EXHAUST &&
                (scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >=
                (scrollState.layoutInfo.totalItemsCount - 1)
        }
    }
    LaunchedEffect(shouldStartPaginate.value) {
        if (shouldStartPaginate.value) viewModel.loadMoreYouTubeItems(continuation)
    }

    // ── Unified list (Xevrae's homeData equivalent) ───────────────────────────
    val allEntries: List<HomeEntry> = remember(quickPicks, homePage) {
        buildList {
            add(HomeEntry.QuickPicksEntry(quickPicks.orEmpty()))
            homePage?.sections?.forEach { add(HomeEntry.SectionEntry(it)) }
        }
    }

    // ── Root layout (exact Xevrae Box structure) ──────────────────────────────
    Box {
        PullToRefreshBox(
            state        = pullToRefreshState,
            onRefresh    = { viewModel.refresh() },
            isRefreshing = isRefreshing,
            indicator    = {
                PullToRefreshDefaults.Indicator(
                    state        = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier     = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(density) { topAppBarHeightPx.toDp() }),
                )
            },
        ) {
            // Xevrae: Crossfade(loading)
            Crossfade(targetState = isLoading, label = "HomeShimmer") { loading ->
                if (!loading) {
                    LazyColumn(
                        state               = scrollState,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        contentPadding      = LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        // Xevrae: itemsIndexed(homeData) { index, item -> ... }
                        itemsIndexed(
                            items = allEntries,
                            key   = { index, entry ->
                                when (entry) {
                                    is HomeEntry.QuickPicksEntry -> "quickpicks"
                                    is HomeEntry.SectionEntry    -> (entry.section.title ?: "") + index
                                }
                            },
                        ) { index, entry ->
                            Box {
                                // Gradient background — index 0 only (exact Xevrae)
                                if (index == 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    colorStops = arrayOf(
                                                        0.0f to animatedColor,
                                                        0.7f to animatedColor.copy(alpha = 0.5f),
                                                        1.0f to Color.Black,
                                                    ),
                                                    start = Offset(0f, 0f),
                                                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                                                )
                                            ),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0x75000000), Color.Black)
                                                )
                                            ),
                                    )
                                }

                                Column(modifier = Modifier.padding(horizontal = 15.dp)) {
                                    if (index == 0) {
                                        Spacer(Modifier.height(with(density) { topAppBarHeightPx.toDp() }))
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    // Account layout — index 0, logged in only (exact Xevrae)
                                    if (index == 0 && accountName != "Guest") {
                                        AccountLayout(accountName = accountName, url = accountImageUrl)
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    when (entry) {
                                        is HomeEntry.QuickPicksEntry -> {
                                            if (entry.songs.isNotEmpty()) {
                                                QuickPicksSection(
                                                    songs           = entry.songs,
                                                    mediaMetadataId = mediaMetadata?.id,
                                                    isPlaying       = isPlaying,
                                                    onPlay          = { song ->
                                                        if (!isListenTogetherGuest) {
                                                            if (song.id == mediaMetadata?.id) {
                                                                playerConnection.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                                            }
                                                        }
                                                    },
                                                    onLongClick = { song ->
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

                                        is HomeEntry.SectionEntry -> {
                                            DareHomeItem(
                                                section       = entry.section,
                                                navController = navController,
                                                mediaMetadataId = mediaMetadata?.id,
                                                currentAlbumId  = mediaMetadata?.album?.id,
                                                isPlaying       = isPlaying,
                                                coroutineScope  = coroutineScope,
                                                onSongLongClick = { item ->
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        when (item) {
                                                            is SongItem     -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                                            is AlbumItem    -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                            is ArtistItem   -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                            else            -> {}
                                                        }
                                                    }
                                                },
                                                onSongClick = { item ->
                                                    when (item) {
                                                        is SongItem -> {
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
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination loading (exact Xevrae AnimatedVisibility pattern)
                        item {
                            AnimatedVisibility(
                                visible = homeListState == ListState.PAGINATING,
                                enter   = expandVertically() + fadeIn(),
                                exit    = fadeOut() + shrinkVertically(),
                            ) {
                                Box(
                                    modifier         = Modifier.fillMaxWidth().height(200.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        // New releases — after pagination exhausted (exact Xevrae pattern)
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
                                item {
                                    Column(Modifier.padding(horizontal = 15.dp)) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text  = "Fresh drops",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                        )
                                        Text(
                                            text    = "New Releases",
                                            style   = MaterialTheme.typography.headlineMedium,
                                            color   = Color.White,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        )
                                        LazyRow {
                                            items(
                                                count = albums.size,
                                                key   = { albums[it].id },
                                            ) { i ->
                                                DareHomeItemContentPlaylist(
                                                    thumbnail = albums[i].thumbnail,
                                                    title     = albums[i].title,
                                                    subtitle  = albums[i].artists?.joinToString { it.name } ?: "",
                                                    onClick   = { navController.navigate("album/${albums[i].id}") },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // EndOfPage (exact Xevrae)
                            item { DareEndOfPage() }
                        }
                    }
                } else {
                    // HomeShimmer (exact Xevrae structure)
                    Column {
                        Spacer(Modifier.height(with(density) { topAppBarHeightPx.toDp() }))
                        DareHomeShimmer()
                    }
                }
            }
        }

        // ── Floating TopAppBar (exact Xevrae AnimatedContent pattern) ──────────
        AnimatedContent(
            targetState    = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
            label          = "topBarBg",
            modifier       = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { topAppBarHeightPx = it.size.height },
        ) { atTop ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (atTop) Color.Transparent else Color.Black.copy(alpha = 0.75f)),
            ) {
                // Hides on scroll down, shows on scroll up (exact Xevrae)
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    DareHomeTopBar(
                        onAccountClick = { showAccountDialog = true },
                        navController  = navController,
                    )
                }
                AnimatedVisibility(
                    visible = !isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Spacer(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars))
                }

                // Chip row — real API chips, functional (Xevrae pattern adapted)
                val chips = homePage?.chips
                if (!chips.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(chipRowState)
                            .padding(vertical = 8.dp, horizontal = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DareChip(
                            text       = "All",
                            isSelected = selectedChip == null,
                            isLoading  = isLoading,
                            onClick    = { viewModel.toggleChip(null) },
                        )
                        chips.forEach { chip ->
                            DareChip(
                                text       = chip.title,
                                isSelected = chip == selectedChip,
                                isLoading  = isLoading,
                                onClick    = { viewModel.toggleChip(chip) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── ListState (Xevrae equivalent) ────────────────────────────────────────────
private enum class ListState { IDLE, LOADING, PAGINATING, PAGINATION_EXHAUST }

// ── HomeTopBar (Xevrae's HomeTopAppBar) ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DareHomeTopBar(onAccountClick: () -> Unit, navController: NavController) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 6..12  -> "Good morning"
        in 13..17 -> "Good afternoon"
        in 18..23 -> "Good evening"
        else      -> "Good night"
    }
    TopAppBar(
        windowInsets = TopAppBarDefaults.windowInsets,
        title = {
            Column {
                Text(
                    text     = "Dare",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(text = greeting, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("history") }) {
                Icon(painterResource(R.drawable.history), null, tint = Color.White)
            }
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(painterResource(R.drawable.settings), null, tint = Color.White)
            }
            IconButton(onClick = onAccountClick) {
                Icon(painterResource(R.drawable.account), null, tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

// ── AccountLayout (exact Xevrae port) ────────────────────────────────────────
@Composable
private fun AccountLayout(accountName: String, url: String?) {
    val context = LocalContext.current
    Column {
        Text(
            text     = "Welcome back",
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            AsyncImage(
                model              = ImageRequest.Builder(context).data(url).crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(40.dp).clip(CircleShape),
            )
            Text(
                text     = accountName,
                style    = MaterialTheme.typography.headlineMedium,
                color    = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ── QuickPicksSection (exact Xevrae QuickPicks composable) ────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPicksSection(
    songs: List<Song>,
    mediaMetadataId: String?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onLongClick: (Song) -> Unit,
) {
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(0.dp) }

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { widthDp = with(density) { it.size.width.toDp() } },
    ) {
        Text(
            text  = "Let's start with a radio",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
        Text(
            text     = "Quick Picks",
            style    = MaterialTheme.typography.headlineMedium,
            color    = Color.White,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        // Exact Xevrae: LazyHorizontalGrid rows=4 height=256.dp with snap
        LazyHorizontalGrid(
            rows   = GridCells.Fixed(4),
            modifier = Modifier.height(256.dp),
            state  = rememberLazyGridState(),
        ) {
            items(songs, key = { it.id }) { song ->
                DareQuickPicksItem(
                    song            = song,
                    isActive        = song.id == mediaMetadataId,
                    widthDp         = widthDp / 2,
                    onClick         = { onPlay(song) },
                    onLongClick     = { onLongClick(song) },
                )
            }
        }
    }
}

// ── QuickPicksItem (exact Xevrae port for Dare's Song model) ──────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DareQuickPicksItem(
    song: Song,
    isActive: Boolean,
    widthDp: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .width(widthDp - 30.dp)
            .focusable(true)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .align(Alignment.CenterVertically)
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(
                Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text     = song.title,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    color    = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                        .padding(bottom = 3.dp),
                )
                Text(
                    text     = song.artists.joinToString { it.name },
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
                )
            }
        }
    }
}

// ── DareHomeItem (Xevrae's HomeItem adapted for HomePage.Section + YTItem) ───
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DareHomeItem(
    section: HomePage.Section,
    navController: NavController,
    mediaMetadataId: String?,
    currentAlbumId: String?,
    isPlaying: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSongClick: (YTItem) -> Unit,
    onSongLongClick: (YTItem) -> Unit,
) {
    if (section.items.isEmpty()) return
    val title = section.title ?: return

    val lazyListState = rememberLazyListState()

    Column {
        // Section header (Xevrae's title/subtitle pattern)
        Column(
            Modifier.padding(start = 10.dp),
        ) {
            if (section.label != null) {
                Text(
                    text  = section.label!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            Text(
                text     = title,
                style    = MaterialTheme.typography.headlineMedium,
                color    = Color.White,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Horizontal scroll of items (Xevrae's LazyRow with snap)
        LazyRow(state = lazyListState) {
            items(
                count = section.items.size,
                key   = { section.items[it].id + it },
            ) { i ->
                val item = section.items[i]
                val isActive = item.id == mediaMetadataId || item.id == currentAlbumId
                when (item) {
                    is SongItem     -> DareHomeItemSong(
                        item        = item,
                        isActive    = isActive,
                        isPlaying   = isPlaying && isActive,
                        onClick     = { onSongClick(item) },
                        onLongClick = { onSongLongClick(item) },
                    )
                    is ArtistItem   -> DareHomeItemArtist(
                        item    = item,
                        onClick = { navController.navigate("artist/${item.id}") },
                    )
                    is AlbumItem    -> DareHomeItemContentPlaylist(
                        thumbnail = item.thumbnail,
                        title     = item.title,
                        subtitle  = item.artists?.joinToString { it.name } ?: "",
                        onClick   = { navController.navigate("album/${item.id}") },
                    )
                    is PlaylistItem -> DareHomeItemContentPlaylist(
                        thumbnail = item.thumbnail ?: "",
                        title     = item.title,
                        subtitle  = item.author?.name ?: "",
                        onClick   = { navController.navigate("online_playlist/${item.id}") },
                    )
                    is PodcastItem  -> DareHomeItemContentPlaylist(
                        thumbnail = item.thumbnail ?: "",
                        title     = item.title,
                        subtitle  = "",
                        onClick   = { navController.navigate("online_podcast/${item.id}") },
                    )
                    is EpisodeItem  -> DareHomeItemSong(
                        item        = SongItem(
                            id        = item.id,
                            title     = item.title,
                            artists   = emptyList(),
                            thumbnail = item.thumbnail ?: "",
                            explicit  = false,
                        ),
                        isActive    = isActive,
                        isPlaying   = isPlaying && isActive,
                        onClick     = { onSongClick(item) },
                        onLongClick = { onSongLongClick(item) },
                    )
                }
            }
        }
    }
}

// ── DareHomeItemSong (Xevrae's HomeItemSong) ──────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DareHomeItemSong(
    item: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.thumbnail)
                    .crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(160.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Text(
                text     = item.title,
                style    = MaterialTheme.typography.titleSmall,
                color    = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(160.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(top = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            Text(
                text     = item.artists?.joinToString { it.name } ?: "",
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(160.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable()
                    .padding(vertical = 3.dp),
            )
        }
    }
}

// ── DareHomeItemContentPlaylist (Xevrae's HomeItemContentPlaylist) ─────────────
@Composable
private fun DareHomeItemContentPlaylist(
    thumbnail: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    thumbSize: Dp = 160.dp,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .wrapContentSize()
            .focusable(true)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnail)
                    .crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(thumbSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                color    = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(thumbSize)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(top = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(thumbSize)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
        }
    }
}

// ── DareHomeItemArtist (Xevrae's HomeItemArtist) ──────────────────────────────
@Composable
private fun DareHomeItemArtist(
    item: ArtistItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.thumbnail)
                    .crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(160.dp)
                    .clip(CircleShape),
            )
            Text(
                text      = item.title,
                style     = MaterialTheme.typography.titleSmall,
                color     = Color.White,
                maxLines  = 1,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .width(160.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(top = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            Text(
                text      = "Artist",
                style     = MaterialTheme.typography.bodySmall,
                maxLines  = 1,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .width(160.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
        }
    }
}

// ── DareChip (Xevrae's Chip composable) ──────────────────────────────────────
@Composable
private fun DareChip(
    text: String,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text  = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ── DareHomeShimmer (exact Xevrae HomeShimmer structure) ──────────────────────
@Composable
private fun DareHomeShimmer() {
    val shimmerColor = Color.White.copy(alpha = 0.07f)
    Column(Modifier.padding(horizontal = 15.dp)) {
        // QuickPicks shimmer (Xevrae's QuickPicksShimmer)
        Box(
            Modifier
                .width(150.dp).height(36.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(10))
                .background(shimmerColor),
        )
        repeat(4) {
            Row(Modifier.height(70.dp).padding(10.dp)) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                Column(Modifier.padding(start = 10.dp).align(Alignment.CenterVertically)) {
                    Box(Modifier.width(200.dp).height(18.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                }
            }
        }
        // Section shimmers (Xevrae's HomeItemShimmer x3)
        repeat(3) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(150.dp).height(36.dp).padding(vertical = 8.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
            LazyRow(userScrollEnabled = false) {
                items(4) {
                    Column(Modifier.height(210.dp).padding(10.dp)) {
                        Box(Modifier.size(160.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                        Spacer(Modifier.size(8.dp))
                        Box(Modifier.width(130.dp).height(16.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                        Spacer(Modifier.size(6.dp))
                        Box(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(10)).background(shimmerColor))
                    }
                }
            }
        }
    }
}

// ── DareEndOfPage (Xevrae's EndOfPage) ───────────────────────────────────────
@Composable
private fun DareEndOfPage() {
    Box(
        modifier          = Modifier.fillMaxWidth().height(280.dp),
        contentAlignment  = Alignment.TopCenter,
    ) {
        Text(
            text      = "© 2026 Dare ${com.dare.music.BuildConfig.VERSION_NAME}\nt4ulquiorra",
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(top = 20.dp).alpha(0.8f),
        )
    }
}
