/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * OnlinePlaylistScreen — Xevrae visual port, Dare data
 */
package com.dare.music.ui.screens.playlist
import com.dare.music.extensions.toMediaItem
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.layout.asPaddingValues

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.animateContentSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CompositionLocalProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.dare.innertube.models.SongItem
import com.dare.music.LocalDatabase
import com.dare.music.LocalListenTogetherManager
import com.dare.music.LocalPlayerAwareWindowInsets
import com.dare.music.LocalPlayerConnection
import com.dare.music.LocalSyncUtils
import com.dare.music.R
import com.dare.music.constants.HideExplicitKey
import com.dare.music.db.entities.PlaylistEntity
import com.dare.music.db.entities.PlaylistSongMap
import com.dare.music.models.toMediaMetadata
import com.dare.music.playback.queues.YouTubePlaylistQueue
import com.dare.music.ui.component.LocalMenuState
import com.dare.music.ui.component.YouTubeListItem
import com.dare.music.ui.menu.YouTubePlaylistMenu
import com.dare.music.ui.menu.YouTubeSongMenu
import com.dare.music.ui.utils.backToMain
import com.dare.music.utils.makeTimeString
import com.dare.music.utils.rememberPreference
import com.dare.music.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// angledGradientBackground (same as AlbumScreen)
private fun Modifier.angledGradientBg(colors: List<Color>, angleDeg: Float): Modifier =
    this.drawBehind {
        if (colors.size < 2) return@drawBehind
        val rad = (angleDeg - 90f) * (PI / 180.0).toFloat()
        val x   = cos(rad) * size.width  / 2f
        val y   = sin(rad) * size.height / 2f
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start  = Offset(size.width / 2f - x, size.height / 2f - y),
                end    = Offset(size.width / 2f + x, size.height / 2f + y),
            )
        )
    }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState     = LocalMenuState.current
    val database      = LocalDatabase.current
    val haptic        = LocalHapticFeedback.current
    val context       = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying     by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist      by viewModel.playlist.collectAsState()
    val songs         by viewModel.playlistSongs.collectAsState()
    val dbPlaylist    by viewModel.dbPlaylist.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hideExplicit  by rememberPreference(HideExplicitKey, false)
    val isPodcast     = viewModel.isPodcastPlaylist

    val snackbarHostState = remember { SnackbarHostState() }

    // Search (exact Xevrae)
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query         by rememberSaveable { mutableStateOf("") }
    val filteredSongs = remember(songs, query) {
        if (query.isEmpty() || !showSearchBar) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter {
            it.second.title.contains(query, true) ||
            it.second.artists.fastAny { a -> a.name.contains(query, true) }
        }
    }
    if (showSearchBar) {
        BackHandler { showSearchBar = false; query = "" }
    }
    LaunchedEffect(showSearchBar) {
        if (showSearchBar) lazyListState.animateScrollToItem(0)
    }

    // Palette colors
    var gradientColors by remember { mutableStateOf(listOf(Color.Black, Color.Black)) }
    LaunchedEffect(playlist?.thumbnail) {
        val url = playlist?.thumbnail ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val req    = ImageRequest.Builder(context).data(url).size(100, 100).allowHardware(false).build()
            val bitmap = runCatching { context.imageLoader.execute(req) }.getOrNull()?.image?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                val raw     = palette.getDominantColor(0xFF000000.toInt())
                val r = ((raw shr 16 and 0xFF) * 0.3f) / 255f
                val g = ((raw shr  8 and 0xFF) * 0.3f) / 255f
                val b = ((raw        and 0xFF) * 0.3f) / 255f
                withContext(Dispatchers.Main) {
                    gradientColors = listOf(Color(r, g, b, 1f), Color.Black)
                }
            }
        }
    }

    // Scroll + TopAppBar
    val lazyListState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) { shouldHideTopBar = !firstItemVisible }

    // Is this playlist currently playing
    val isThisPlaylistPlaying = isPlaying && playlist?.let { pl ->
        songs.any { it.id == mediaMetadata?.id }
    } == true

    // Pagination (exact Xevrae shouldStartPaginate)
    val shouldStartPaginate by remember {
        derivedStateOf {
            !isLoadingMore &&
            viewModel.continuation != null &&
            (lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >=
            (lazyListState.layoutInfo.totalItemsCount - 6)
        }
    }
    LaunchedEffect(shouldStartPaginate) {
        if (shouldStartPaginate) viewModel.loadMoreSongs()
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(isLoading && playlist == null, label = "playlistLoad") { loading ->
            if (loading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else {
                val pl = playlist
                if (pl == null) {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                } else {
                    LazyColumn(
                        modifier       = Modifier.fillMaxWidth().background(Color.Black),
                        state          = lazyListState,
                        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    ) {
                        // ── Header or Search bar (exact Xevrae) ───────────
                        if (!showSearchBar) {
                            item(contentType = "header") {
                                Box(Modifier.fillMaxWidth().wrapContentHeight().background(Color.Transparent)) {
                                    Box(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier.fillMaxWidth().height(260.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .angledGradientBg(gradientColors, 25f),
                                        )
                                        Box(
                                            Modifier.fillMaxWidth().height(180.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0x75000000), Color.Black)
                                                    )
                                                ),
                                        )
                                    }
                                    Column(Modifier.background(Color.Transparent)) {
                                        // Back + Search row
                                        Row(
                                            Modifier.wrapContentWidth().padding(16.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars),
                                        ) {
                                            IconButton(onClick = { navController.navigateUp() }) {
                                                Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            IconButton(onClick = { showSearchBar = true }) {
                                                Icon(Icons.Rounded.Search, null, tint = Color.White)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.Start) {
                                            // Playlist art 250dp (exact Xevrae)
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(pl.thumbnail).crossfade(true).build(),
                                                contentDescription = null,
                                                contentScale       = ContentScale.FillHeight,
                                                modifier           = Modifier
                                                    .height(250.dp)
                                                    .wrapContentWidth()
                                                    .align(Alignment.CenterHorizontally)
                                                    .clip(RoundedCornerShape(8.dp)),
                                            )
                                            Box(Modifier.fillMaxWidth().wrapContentHeight()) {
                                                Column(Modifier.padding(horizontal = 32.dp)) {
                                                    Spacer(Modifier.size(25.dp))
                                                    // Title (Xevrae: titleMedium, maxLines 2)
                                                    Text(
                                                        text     = pl.title,
                                                        style    = MaterialTheme.typography.titleMedium,
                                                        color    = Color.White,
                                                        maxLines = 2,
                                                    )
                                                    Column(Modifier.padding(vertical = 4.dp)) {
                                                        // Author clickable (exact Xevrae TextButton)
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            TextButton(
                                                                modifier       = Modifier.wrapContentHeight().defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
                                                                contentPadding = PaddingValues(vertical = 1.dp),
                                                                onClick        = {
                                                                    pl.author?.id?.let { navController.navigate("artist/$it") }
                                                                },
                                                            ) {
                                                                Text(
                                                                    text  = pl.author?.name ?: "",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.White,
                                                                )
                                                            }
                                                        }
                                                        Spacer(Modifier.size(4.dp))
                                                        // Type + year (exact Xevrae)
                                                        Text(
                                                            text  = "${stringResource(R.string.playlist)}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color(0xC4FFFFFF),
                                                        )
                                                    }
                                                    // Action buttons (exact Xevrae row)
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        // Play / Pause (Xevrae: Crossfade)
                                                        Crossfade(isThisPlaylistPlaying, label = "playPause") { playing ->
                                                            IconButton(
                                                                onClick = {
                                                                    if (!isGuest && songs.isNotEmpty()) {
                                                                        if (playing) playerConnection.togglePlayPause()
                                                                        else playerConnection.playQueue(
                                                                            YouTubePlaylistQueue(
                                                                                playlistId          = pl.id,
                                                                                playlistTitle       = pl.title,
                                                                                initialSongs        = songs,
                                                                                initialContinuation = viewModel.continuation,
                                                                            )
                                                                        )
                                                                    }
                                                                },
                                                                modifier = Modifier.size(48.dp),
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                                                                    tint    = MaterialTheme.colorScheme.primary,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                )
                                                            }
                                                        }
                                                        // Heart (Xevrae: HeartCheckBox size 32)
                                                        IconButton(
                                                            onClick  = {
                                                                val curr = dbPlaylist
                                                                if (curr != null) {
                                                                    database.transaction { update(curr.playlist.toggleLike()) }
                                                                } else {
                                                                    database.transaction {
                                                                        val entity = PlaylistEntity(
                                                                            name              = pl.title,
                                                                            browseId          = pl.id,
                                                                            thumbnailUrl      = pl.thumbnail,
                                                                            isEditable        = pl.isEditable,
                                                                            remoteSongCount   = pl.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                                                                            playEndpointParams    = pl.playEndpoint?.params,
                                                                            shuffleEndpointParams = pl.shuffleEndpoint?.params,
                                                                            radioEndpointParams   = pl.radioEndpoint?.params,
                                                                        ).toggleLike()
                                                                        insert(entity)
                                                                        coroutineScope.launch(Dispatchers.IO) {
                                                                            songs.map { it.toMediaMetadata() }.onEach(::insert)
                                                                                .mapIndexed { i, s -> PlaylistSongMap(songId = s.id, playlistId = entity.id, position = i, setVideoId = s.setVideoId) }
                                                                                .forEach(::insert)
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.size(32.dp),
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(
                                                                    if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite
                                                                    else R.drawable.favorite_border
                                                                ),
                                                                tint     = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else Color.White,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                            )
                                                        }
                                                        Spacer(Modifier.weight(1f))
                                                        // Radio (Xevrae: baseline_sensors_24)
                                                        pl.radioEndpoint?.let { ep ->
                                                            IconButton(
                                                                onClick  = { if (!isGuest) playerConnection.playQueue(YouTubePlaylistQueue(playlistId = ep.playlistId ?: pl.id, playlistTitle = pl.title, initialSongs = songs, initialContinuation = null)) },
                                                                modifier = Modifier.size(36.dp),
                                                            ) { Icon(Icons.Outlined.Sensors, null, modifier = Modifier.fillMaxSize()) }
                                                            Spacer(Modifier.size(5.dp))
                                                        }
                                                        // Shuffle
                                                        pl.shuffleEndpoint?.let { ep ->
                                                            IconButton(
                                                                onClick  = {
                                                                    if (!isGuest) playerConnection.playQueue(
                                                                        YouTubePlaylistQueue(playlistId = ep.playlistId ?: pl.id, playlistTitle = pl.title, initialSongs = songs.shuffled(), initialContinuation = null)
                                                                    )
                                                                },
                                                                modifier = Modifier.size(36.dp),
                                                            ) { Icon(Icons.Outlined.Shuffle, null, modifier = Modifier.fillMaxSize()) }
                                                            Spacer(Modifier.size(5.dp))
                                                        }
                                                        // More
                                                        IconButton(
                                                            onClick  = {
                                                                menuState.show {
                                                                    YouTubePlaylistMenu(playlist = pl, songs = songs, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp),
                                                        ) { Icon(painterResource(R.drawable.more_vert), null, modifier = Modifier.fillMaxSize()) }
                                                    }
                                                    // Track count (Xevrae: bodyMedium White)
                                                    Text(
                                                        text     = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                                                        color    = Color.White,
                                                        style    = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Search header (exact Xevrae stickyHeader)
                            stickyHeader {
                                Box(Modifier.background(Color.Black)) {
                                    Row(
                                        Modifier.wrapContentWidth().padding(16.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                    ) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White)
                                        }
                                        SearchBar(
                                            modifier    = Modifier.height(50.dp).padding(horizontal = 12.dp).weight(1f),
                                            colors      = SearchBarDefaults.colors(containerColor = Color.Transparent),
                                            inputField  = {
                                                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                                                    SearchBarDefaults.InputField(
                                                        query           = query,
                                                        onQueryChange   = { query = it },
                                                        onSearch        = { showSearchBar = false },
                                                        expanded        = showSearchBar,
                                                        onExpandedChange = { showSearchBar = it },
                                                        placeholder     = { Text(stringResource(R.string.search), style = MaterialTheme.typography.bodyMedium) },
                                                    )
                                                }
                                            },
                                            expanded        = false,
                                            onExpandedChange = {},
                                            windowInsets    = WindowInsets(0, 0, 0, 0),
                                        ) {}
                                        IconButton(onClick = { showSearchBar = false; query = "" }) {
                                            Icon(Icons.Rounded.Close, null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // ── Song list (Xevrae: SongFullWidthItems with swipe to queue) ──
                        itemsIndexed(
                            items = filteredSongs,
                            key   = { _, (_, s) -> s.id },
                        ) { _, (origIndex, song) ->
                            if (!hideExplicit || !song.explicit) {
                                XevPlaylistSongRow(
                                    title        = song.title,
                                    artists      = song.artists.joinToString { it.name },
                                    thumbnail    = song.thumbnail,
                                    isPlaying    = isPlaying && mediaMetadata?.id == song.id,
                                    isActive     = mediaMetadata?.id == song.id,
                                    onPlay       = {
                                        if (!isGuest) {
                                            if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else playerConnection.playQueue(
                                                YouTubePlaylistQueue(
                                                    playlistId          = pl.id,
                                                    playlistTitle       = pl.title,
                                                    initialSongs        = filteredSongs.map { it.second },
                                                    initialContinuation = viewModel.continuation,
                                                    startIndex          = origIndex,
                                                )
                                            )
                                        }
                                    },
                                    onMore       = {
                                        menuState.show {
                                            YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                        }
                                    },
                                    modifier     = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Pagination states (exact Xevrae)
                        if (isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(32.dp))
                                }
                            }
                        }
                        item { XevPlaylistEndOfPage() }
                    }

                    // Animated TopAppBar (exact Xevrae: fadeIn + slideInVertically + angledGradient)
                    AnimatedVisibility(
                        visible = shouldHideTopBar && !showSearchBar,
                        enter   = fadeIn() + slideInVertically(),
                        exit    = fadeOut() + slideOutVertically(),
                    ) {
                        TopAppBar(
                            windowInsets   = TopAppBarDefaults.windowInsets.exclude(
                                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start)
                            ),
                            title          = {
                                Text(
                                    text     = pl.title,
                                    style    = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.CenterVertically)
                                        .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately).focusable(),
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    IconButton(onClick = { navController.navigateUp() }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSearchBar = true }) {
                                    Icon(Icons.Rounded.Search, null, tint = Color.White)
                                }
                            },
                            colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            modifier = Modifier.angledGradientBg(gradientColors, 90f),
                        )
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Song row with swipe-to-queue (exact Xevrae SongFullWidthItems) ─────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XevPlaylistSongRow(
    title        : String,
    artists      : String,
    thumbnail    : String,
    isPlaying    : Boolean,
    isActive     : Boolean,
    onPlay       : () -> Unit,
    onMore       : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density        = LocalDensity.current
    val maxOffset      = 360f
    val offsetX        = remember { Animatable(0f) }
    var heightDp       by remember { mutableStateOf(0.dp) }

    Box(modifier = modifier) {
        Crossfade(offsetX.value >= maxOffset / 2, label = "swipe") { show ->
            if (show) {
                Box(Modifier.height(heightDp).padding(start = 15.dp), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue", tint = Color.White)
                }
            }
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .combinedClickable(onClick = onPlay, onLongClick = onMore)
                .animateContentSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, drag ->
                            if (offsetX.value + drag > 0) {
                                change.consume()
                                coroutineScope.launch { offsetX.snapTo((offsetX.value + drag).coerceAtMost(maxOffset)) }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        },
                    )
                }
                .onGloballyPositioned { heightDp = with(density) { it.size.height.toDp() } }
                .padding(vertical = 6.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    Icon(painterResource(R.drawable.pause), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(thumbnail).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                    )
                }
            }
            Column(
                Modifier.weight(1f).padding(start = 12.dp, end = 10.dp).align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    color    = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.CenterVertically)
                        .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately).focusable()
                        .padding(bottom = 3.dp),
                )
                Text(
                    text     = artists,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color    = Color(0xC4FFFFFF),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.CenterVertically)
                        .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately).focusable(),
                )
            }
            IconButton(onClick = onMore) {
                Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun XevPlaylistEndOfPage() {
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text      = "© 2026 Dare ${com.dare.music.BuildConfig.VERSION_NAME}\nt4ulquiorra",
            style     = MaterialTheme.typography.bodySmall,
            color     = Color.Gray,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(top = 20.dp).alpha(0.8f),
        )
    }
}
