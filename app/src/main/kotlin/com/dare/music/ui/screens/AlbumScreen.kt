/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * AlbumScreen — Xevrae visual port, Dare data
 */
package com.dare.music.ui.screens

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.animateContentSize
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.dare.music.LocalDatabase
import com.dare.music.LocalDownloadUtil
import com.dare.music.LocalListenTogetherManager
import com.dare.music.LocalPlayerAwareWindowInsets
import com.dare.music.LocalPlayerConnection
import com.dare.music.R
import com.dare.music.constants.HideExplicitKey
import com.dare.music.constants.HideVideoSongsKey
import com.dare.music.extensions.toMediaItem
import com.dare.music.playback.queues.LocalAlbumRadio
import com.dare.music.ui.component.ExpandableText
import com.dare.music.ui.component.LocalMenuState
import com.dare.music.ui.component.YouTubeGridItem
import com.dare.music.ui.menu.SongMenu
import com.dare.music.ui.menu.YouTubeAlbumMenu
import com.dare.music.ui.utils.backToMain
import com.dare.music.utils.makeTimeString
import com.dare.music.utils.rememberPreference
import com.dare.music.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ── angledGradientBackground (Xevrae extension) ───────────────────────────────
private fun Modifier.angledGradientBackground(colors: List<Color>, angleDeg: Float): Modifier =
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
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context       = LocalContext.current
    val menuState     = LocalMenuState.current
    val database      = LocalDatabase.current
    val haptic        = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val downloadUtil  = LocalDownloadUtil.current

    val isPlaying     by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlistId    by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions  by viewModel.otherVersions.collectAsState()
    val hideExplicit   by rememberPreference(HideExplicitKey, false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, false)

    val filteredSongs = remember(albumWithSongs, hideExplicit, hideVideoSongs) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) songs = songs.filter { !it.song.explicit }
        if (hideVideoSongs) songs = songs.filter { !it.song.isVideo }
        songs
    }

    // Download state
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    LaunchedEffect(albumWithSongs) {
        val songIds = albumWithSongs?.songs?.map { it.id }
        if (songIds.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = when {
                songIds.all { downloads[it]?.state == Download.STATE_COMPLETED }    -> Download.STATE_COMPLETED
                songIds.all {
                    downloads[it]?.state == Download.STATE_QUEUED ||
                    downloads[it]?.state == Download.STATE_DOWNLOADING ||
                    downloads[it]?.state == Download.STATE_COMPLETED
                } -> Download.STATE_DOWNLOADING
                else -> Download.STATE_STOPPED
            }
        }
    }

    // Palette colors (Xevrae: extracted from album art)
    var gradientColors by remember { mutableStateOf(listOf(Color.Black, Color.Black)) }
    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        val url = albumWithSongs?.album?.thumbnailUrl ?: return@LaunchedEffect
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
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { lazyState.firstVisibleItemIndex == 0 } }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) { shouldHideTopBar = !firstItemVisible }

    // Is this album currently playing
    val isThisAlbumPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs?.album?.id

    Crossfade(albumWithSongs == null, label = "albumLoad") { loading ->
        if (loading) {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        } else {
            val aws = albumWithSongs!!

            LazyColumn(
                modifier       = Modifier.fillMaxWidth().background(Color.Black),
                state          = lazyState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                // ── Header (exact Xevrae structure) ───────────────────────
                item(contentType = "header") {
                    Box(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.Transparent),
                    ) {
                        // Gradient background box (Xevrae: 260dp angledGradient + 180dp fade to black)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .angledGradientBackground(gradientColors, 25f),
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
                        Column(Modifier.background(Color.Transparent)) {
                            // Back button row
                            Row(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.statusBars),
                            ) {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White)
                                }
                            }
                            // Album art + info
                            Column(horizontalAlignment = Alignment.Start) {
                                // Album art: height 250dp, wrapContentWidth, centered (exact Xevrae)
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(aws.album.thumbnailUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale       = ContentScale.FillHeight,
                                    modifier           = Modifier
                                        .height(250.dp)
                                        .wrapContentWidth()
                                        .align(Alignment.CenterHorizontally)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                    Column(Modifier.padding(horizontal = 32.dp)) {
                                        Spacer(Modifier.size(25.dp))
                                        // Title (Xevrae: titleLarge, maxLines 2)
                                        Text(
                                            text     = aws.album.title,
                                            style    = MaterialTheme.typography.titleLarge,
                                            color    = Color.White,
                                            maxLines = 2,
                                        )
                                        Column(Modifier.padding(vertical = 8.dp)) {
                                            // Artist name — clickable (exact Xevrae)
                                            Text(
                                                text     = aws.artists.joinToString { it.name },
                                                style    = MaterialTheme.typography.titleSmall,
                                                color    = Color.White,
                                                modifier = Modifier.clickable {
                                                    aws.artists.firstOrNull()?.id?.let {
                                                        navController.navigate("artist/$it")
                                                    }
                                                },
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            // Year + Album type (Xevrae: bodyMedium, 0xC4FFFFFF)
                                            Text(
                                                text  = "${aws.album.year ?: ""} • ${stringResource(R.string.album)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xC4FFFFFF),
                                            )
                                        }
                                        // Action buttons row (Xevrae: play 48dp, download 36dp, heart 36dp, shuffle 36dp)
                                        Row(
                                            modifier          = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // Play / Pause (Xevrae: Crossfade on isThisPlaying)
                                            Crossfade(isThisAlbumPlaying, label = "playPause") { playing ->
                                                IconButton(
                                                    onClick = {
                                                        if (!isGuest) {
                                                            if (playing) playerConnection.togglePlayPause()
                                                            else {
                                                                playerConnection.service.getAutomix(playlistId)
                                                                playerConnection.playQueue(LocalAlbumRadio(aws))
                                                            }
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
                                            Spacer(Modifier.size(5.dp))
                                            // Download (Xevrae: Crossfade STATE_DOWNLOADED / DOWNLOADING / else)
                                            Crossfade(downloadState, label = "download") { state ->
                                                when (state) {
                                                    Download.STATE_COMPLETED -> {
                                                        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                                            Icon(painterResource(R.drawable.offline), tint = Color(0xFF00A0CB), contentDescription = null, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                        }
                                                    }
                                                    Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                                                        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                                                            Icon(painterResource(R.drawable.downloading), tint = MaterialTheme.colorScheme.primary, contentDescription = null, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                        }
                                                    }
                                                    else -> {
                                                        IconButton(
                                                            onClick  = {
                                                                coroutineScope.launch {
                                                                    aws.songs.forEach { downloadUtil.download(it.toMediaItem()) }
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp),
                                                        ) {
                                                            Icon(painterResource(R.drawable.download), null, modifier = Modifier.fillMaxSize())
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.size(5.dp))
                                            // Heart / like (Xevrae: HeartCheckBox size 36)
                                            IconButton(
                                                onClick = {
                                                    database.query { update(aws.album.toggleLike()) }
                                                },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (aws.album.bookmarkedAt != null) R.drawable.favorite
                                                        else R.drawable.favorite_border
                                                    ),
                                                    tint     = if (aws.album.bookmarkedAt != null) MaterialTheme.colorScheme.error
                                                               else Color.White,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Spacer(Modifier.size(5.dp))
                                            // Shuffle (Xevrae: RippleIconButton size 36)
                                            IconButton(
                                                onClick = {
                                                    if (!isGuest) {
                                                        playerConnection.playQueue(
                                                            LocalAlbumRadio(aws.copy(songs = aws.songs.shuffled()))
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        // Description (Xevrae: DescriptionView → Dare: ExpandableText)
                                        ExpandableText(
                                            text             = aws.album.description.orEmpty().ifEmpty { stringResource(R.string.no_description) },
                                            collapsedMaxLines = 3,
                                            modifier         = Modifier.padding(vertical = 8.dp),
                                        )
                                        // Track count + total duration (Xevrae: bodyMedium White)
                                        val totalDuration = aws.songs.sumOf { it.song.duration }
                                        Text(
                                            text     = "${pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size)} • ${makeTimeString(totalDuration * 1000L)}",
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

                // ── Song list with index (Xevrae: SongFullWidthItems with index) ──
                itemsIndexed(
                    items = filteredSongs,
                    key   = { _, song -> song.id },
                ) { index, song ->
                    XevAlbumSongRow(
                        index     = index,
                        title     = song.title,
                        artists   = song.artists.joinToString { it.name },
                        isPlaying = isPlaying && mediaMetadata?.id == song.id,
                        isActive  = mediaMetadata?.id == song.id,
                        onPlay    = {
                            if (!isGuest) {
                                if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                else {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(LocalAlbumRadio(aws, startIndex = index))
                                }
                            }
                        },
                        onMore    = {
                            menuState.show {
                                SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                            }
                        },
                        onAddToQueue = {
                            playerConnection.player.addMediaItem(song.toMediaItem())
                        },
                    )
                }

                // ── Other versions (Xevrae: AnimatedVisibility + LazyRow HomeItemContentPlaylist) ──
                if (otherVersions.isNotEmpty()) {
                    item(contentType = "other_version") {
                        AnimatedVisibility(otherVersions.isNotEmpty()) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text     = stringResource(R.string.other_versions),
                                    style    = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                )
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    items(otherVersions) { album ->
                                        Box(
                                            modifier = Modifier
                                                .wrapContentSize()
                                                .focusable(true)
                                                .combinedClickable(
                                                    onClick = { navController.navigate("album/${album.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                ),
                                        ) {
                                            Column(Modifier.padding(10.dp)) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(album.thumbnail).crossfade(true).build(),
                                                    contentDescription = null,
                                                    contentScale       = ContentScale.Crop,
                                                    modifier           = Modifier.size(180.dp).clip(RoundedCornerShape(10.dp)),
                                                )
                                                Text(album.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, modifier = Modifier.width(180.dp).padding(top = 8.dp).basicMarquee(Int.MAX_VALUE).focusable())
                                                Text(album.artists?.joinToString { it.name } ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.width(180.dp).basicMarquee(Int.MAX_VALUE).focusable())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // EndOfPage (Xevrae)
                item { XevAlbumEndOfPage() }
            }

            // Animated TopAppBar (exact Xevrae: fadeIn + slideInVertically, angledGradient)
            AnimatedVisibility(
                visible = shouldHideTopBar,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut() + slideOutVertically(),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text     = aws.album.title,
                            style    = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(Alignment.CenterVertically)
                                .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                .focusable(),
                        )
                    },
                    navigationIcon = {
                        Box(Modifier.padding(horizontal = 5.dp)) {
                            IconButton(
                                onClick     = { navController.navigateUp() },
                                modifier    = Modifier.size(32.dp),
                            ) { Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
                        }
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.angledGradientBackground(gradientColors, 90f),
                )
            }
        }
    }
}

// ── AlbumSongRow with index number (Xevrae's SongFullWidthItems with index) ───
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XevAlbumSongRow(
    index        : Int,
    title        : String,
    artists      : String,
    isPlaying    : Boolean,
    isActive     : Boolean,
    onPlay       : () -> Unit,
    onMore       : () -> Unit,
    onAddToQueue : () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density        = LocalDensity.current
    val maxOffset      = 360f
    val offsetX        = remember { Animatable(0f) }
    var heightDp       by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxWidth()) {
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
                            if (offsetX.value >= maxOffset) onAddToQueue()
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        },
                    )
                }
                .onGloballyPositioned { heightDp = with(density) { it.size.height.toDp() } }
                .padding(vertical = 6.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(8.dp))
            // Index number or playing indicator (exact Xevrae)
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    Icon(painterResource(R.drawable.pause), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text  = (index + 1).toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Column(
                Modifier.weight(1f).padding(start = 12.dp, end = 10.dp).align(Alignment.CenterVertically),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
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
private fun XevAlbumEndOfPage() {
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text      = "© 2026 Dare ${com.dare.music.BuildConfig.VERSION_NAME}\nt4ulquiorra",
            style     = MaterialTheme.typography.bodySmall,
            color     = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier  = Modifier.padding(top = 20.dp).alpha(0.8f),
        )
    }
}
