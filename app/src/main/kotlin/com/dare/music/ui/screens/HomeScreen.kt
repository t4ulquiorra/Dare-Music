/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * HomeScreen — ported from Xevrae/SimpMusic, exact feature/visual parity
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.dare.music.ui.component.YouTubeGridItem
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

// ── Sealed list entry — mirrors Xevrae's homeData list approach ───────────────
private sealed class HomeEntry {
    data class QuickPicksEntry(val songs: List<Song>) : HomeEntry()
    data class SectionEntry(val section: HomePage.Section) : HomeEntry()
}

// ── isScrollingUp — exact Xevrae extension ────────────────────────────────────
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

    // ── Pull to refresh ───────────────────────────────────────────────────────
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) coroutineScope.launch { pullToRefreshState.animateToHidden() }
    }

    // ── TopAppBar height ──────────────────────────────────────────────────────
    var topAppBarHeightPx by remember { mutableIntStateOf(0) }

    // ── Dominant color extraction (Xevrae's mainHomeThumbnail pattern) ────────
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
                val r = ((raw shr 16 and 0xFF) * 0.3f) / 255f
                val g = ((raw shr  8 and 0xFF) * 0.3f) / 255f
                val b = ((raw        and 0xFF) * 0.3f) / 255f
                withContext(Dispatchers.Main) { topHeaderColor = Color(r, g, b, 1f) }
            }
        }
    }

    // ── Pagination (Xevrae's shouldStartPaginate pattern) ────────────────────
    val continuation = homePage?.continuation
    val shouldStartPaginate = remember {
        derivedStateOf {
            continuation != null &&
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

    // ── ytGridItem lambda ─────────────────────────────────────────────────────
    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item           = item,
            isActive       = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying      = isPlaying,
            coroutineScope = coroutineScope,
            thumbnailRatio = 1f,
            modifier = Modifier.combinedClickable(
                onClick = {
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
                onLongClick = {
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
            ),
        )
    }

    // ── Root layout ───────────────────────────────────────────────────────────
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
            // Xevrae's Crossfade(loading) pattern
            Crossfade(targetState = isLoading, label = "HomeShimmer") { loading ->
                if (!loading) {
                    LazyColumn(
                        state               = scrollState,
                        verticalArrangement = Arrangement.spacedBy(28.dp),
                        contentPadding      = LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    ) {
                        // Xevrae's itemsIndexed pattern
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
                                // Gradient background — only index 0, exact Xevrae pattern
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
                                    // Account layout — only at index 0 and only if logged in
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
                                                    onLongClick     = { song ->
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
                                            HomeSectionItem(
                                                section    = entry.section,
                                                ytGridItem = ytGridItem,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination loading indicator
                        item {
                            AnimatedVisibility(
                                visible = continuation != null,
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

                        // New releases — shown after pagination exhausted
                        if (continuation == null) {
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
                                            text       = "New Releases",
                                            style      = MaterialTheme.typography.headlineMedium,
                                            color      = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines   = 1,
                                            modifier   = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(count = albums.size, key = { albums[it].id }) { i ->
                                                Box(Modifier.width(160.dp)) { ytGridItem(albums[i]) }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                } else {
                    // Loading shimmer
                    Column {
                        Spacer(Modifier.height(with(density) { topAppBarHeightPx.toDp() }))
                        HomeShimmer()
                    }
                }
            }
        }

        // ── Floating TopAppBar — exact Xevrae AnimatedContent pattern ─────────
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
                // Hides on scroll down, shows on scroll up
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    HomeTopBar(onAccountClick = { showAccountDialog = true }, navController = navController)
                }
                AnimatedVisibility(
                    visible = !isScrollingUp,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Spacer(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars))
                }
                // Chip row — real API chips, functional
                val chips = homePage?.chips
                if (!chips.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(chipRowState)
                            .padding(vertical = 8.dp, horizontal = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HomeChip(text = "All", isSelected = selectedChip == null, isLoading = isLoading) {
                            viewModel.toggleChip(null)
                        }
                        chips.forEach { chip ->
                            HomeChip(text = chip.title, isSelected = chip == selectedChip, isLoading = isLoading) {
                                viewModel.toggleChip(chip)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── HomeTopBar — Xevrae's HomeTopAppBar ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onAccountClick: () -> Unit, navController: NavController) {
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
                Text(text = "Dare", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 2.dp))
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

// ── AccountLayout — exact Xevrae port ────────────────────────────────────────
@Composable
private fun AccountLayout(accountName: String, url: String?) {
    val context = LocalContext.current
    Column {
        Text(text = "Welcome back", style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(bottom = 3.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp)) {
            AsyncImage(
                model              = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(40.dp).clip(CircleShape),
            )
            Text(text = accountName, style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

// ── QuickPicksSection — Xevrae's QuickPicks composable ───────────────────────
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
        Text(text = "Let's start with a radio", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            text       = "Quick Picks",
            style      = MaterialTheme.typography.headlineMedium,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows                  = GridCells.Fixed(4),
            modifier              = Modifier.height(256.dp),
            state                 = rememberLazyGridState(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                QuickPicksItem(
                    song            = song,
                    isActive        = song.id == mediaMetadataId,
                    isPlaying       = isPlaying && song.id == mediaMetadataId,
                    widthDp         = widthDp / 2,
                    onClick         = { onPlay(song) },
                    onLongClick     = { onLongClick(song) },
                )
            }
        }
    }
}

// ── QuickPicksItem ────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPicksItem(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    widthDp: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .width(widthDp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model              = ImageRequest.Builder(context).data(song.thumbnailUrl).crossfade(true).build(),
            contentDescription = null,
            modifier           = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
            contentScale       = ContentScale.Crop,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = song.title,
                color      = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = song.artists.joinToString { it.name },
                color    = Color.Gray,
                style    = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── HomeSectionItem — renders one HomePage.Section ───────────────────────────
@Composable
private fun HomeSectionItem(section: HomePage.Section, ytGridItem: @Composable (YTItem) -> Unit) {
    if (section.items.isEmpty()) return
    val title = section.title ?: return
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.headlineMedium,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(count = section.items.size, key = { section.items[it].id + it }) { i ->
                Box(Modifier.width(160.dp)) { ytGridItem(section.items[i]) }
            }
        }
    }
}

// ── HomeChip ──────────────────────────────────────────────────────────────────
@Composable
private fun HomeChip(text: String, isSelected: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text       = text,
            color      = Color.White,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ── HomeShimmer ───────────────────────────────────────────────────────────────
@Composable
private fun HomeShimmer() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.4f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)))
        Box(Modifier.fillMaxWidth().height(256.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)))
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Box(Modifier.width(160.dp).height(160.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)))
                    }
                }
            }
        }
    }
}
