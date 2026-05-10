/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * ArtistScreen — exact Xevrae/SimpMusic visual and feature parity
 */
package com.dare.music.ui.screens.artist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.allowHardware
import coil3.toBitmap
import com.dare.innertube.models.AlbumItem
import com.dare.innertube.models.ArtistItem
import com.dare.innertube.models.EpisodeItem
import com.dare.innertube.models.PlaylistItem
import com.dare.innertube.models.SongItem
import com.dare.innertube.models.WatchEndpoint
import com.dare.innertube.models.YTItem
import com.dare.music.LocalListenTogetherManager
import com.dare.music.LocalPlayerConnection
import com.dare.music.R
import com.dare.music.extensions.toMediaItem
import com.dare.music.models.toMediaMetadata
import com.dare.music.playback.queues.YouTubeQueue
import com.dare.music.ui.component.ExpandableText
import com.dare.music.ui.component.LocalMenuState
import com.dare.music.ui.menu.YouTubeAlbumMenu
import com.dare.music.ui.menu.YouTubeArtistMenu
import com.dare.music.ui.menu.YouTubePlaylistMenu
import com.dare.music.ui.menu.YouTubeSongMenu
import com.dare.music.ui.utils.backToMain
import com.dare.music.viewmodels.ArtistViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── rgbFactor extension (Xevrae) ─────────────────────────────────────────────
private fun Color.rgbFactor(factor: Float) = Color(
    red   = (red   * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue  = (blue  * factor).coerceIn(0f, 1f),
    alpha = alpha,
)

// ── Title animation constants (exact Xevrae values) ───────────────────────────
private const val TITLE_FONT_SCALE_START = 1f
private const val TITLE_FONT_SCALE_END   = 0.46f
private val TITLE_PADDING_START          = 20.dp
private val TITLE_PADDING_END            = 72.dp

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val menuState             = LocalMenuState.current
    val haptic                = LocalHapticFeedback.current
    val coroutineScope        = rememberCoroutineScope()
    val playerConnection      = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest               = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val isPlaying           by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata       by playerConnection.mediaMetadata.collectAsState()
    val isChannelSubscribed by viewModel.isChannelSubscribed.collectAsState()

    val artistPage = viewModel.artistPage

    // Xevrae: Crossfade(state) { Loading / Success / Error }
    Crossfade(artistPage == null, label = "artistLoad") { loading ->
        if (loading) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                IconButton(
                    onClick  = { navController.navigateUp() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() })
                        .padding(4.dp),
                ) { Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White) }
            }
        } else {
            val page = artistPage!!
            // Xevrae: CollapsingToolbarParallaxEffect(title, imageUrl, onBack) { color -> }
            XevCollapsingToolbar(
                title    = page.artist.title,
                imageUrl = page.artist.thumbnail,
                onBack   = { navController.navigateUp() },
            ) { paletteColor ->
                Column {
                    // ── Subscribers + play count (exact Xevrae row) ───────
                    Column(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        Row {
                            Text(
                                text      = page.subscriberCountText ?: "",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = Color.White,
                                textAlign = TextAlign.Start,
                                modifier  = Modifier.weight(1f),
                            )
                            Text(
                                text      = page.monthlyListenerCount ?: "",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = Color.White,
                                textAlign = TextAlign.End,
                                modifier  = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // ── Follow + Shuffle + Radio (exact Xevrae row) ───
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Follow/Unfollow with animated sweep border
                            XevAnimatedBorderButton(
                                isAnimated = !isChannelSubscribed,
                                onClick    = { viewModel.toggleChannelSubscription() },
                            ) {
                                Text(
                                    text  = if (isChannelSubscribed) stringResource(R.string.subscribed)
                                            else stringResource(R.string.subscribe),
                                    color = Color.White,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            // Shuffle
                            page.artist.shuffleEndpoint?.let { ep ->
                                IconButton(onClick = {
                                    if (!isGuest) playerConnection.playQueue(YouTubeQueue(ep))
                                }) { Icon(Icons.Outlined.Shuffle, "Shuffle", tint = Color.White) }
                            }
                            Spacer(Modifier.weight(1f))
                            // Start Radio
                            page.artist.radioEndpoint?.let { ep ->
                                TextButton(
                                    onClick = {
                                        if (!isGuest) playerConnection.playQueue(YouTubeQueue(ep))
                                    },
                                    colors  = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Sensors, "")
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.radio))
                                    }
                                }
                            }
                        }
                    }

                    // ── Sections (Xevrae iterates by content type) ────────
                    page.sections.forEach { section ->
                        if (section.items.isEmpty()) return@forEach
                        val title = section.title ?: return@forEach

                        val isSongSection = section.items.firstOrNull() is SongItem &&
                            (section.items.firstOrNull() as? SongItem)?.album != null

                        // Xevrae: Popular songs → vertical SongFullWidthItems list
                        // All others → horizontal LazyRow of HomeItemContentPlaylist / HomeItemArtist
                        if (isSongSection) {
                            // Exact Xevrae: header row + forEach songs
                            AnimatedVisibility(section.items.isNotEmpty()) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier          = Modifier.padding(horizontal = 20.dp),
                                    ) {
                                        Text(
                                            text     = title,
                                            style    = MaterialTheme.typography.labelMedium,
                                            color    = Color.White,
                                            modifier = Modifier.weight(1f),
                                        )
                                        section.moreEndpoint?.let { ep ->
                                            TextButton(
                                                onClick = {
                                                    navController.navigate(
                                                        "artist/${viewModel.artistId}/items?browseId=${ep.browseId}?params=${ep.params}"
                                                    )
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                            ) { Text("More", style = MaterialTheme.typography.bodySmall) }
                                        }
                                    }
                                    section.items.filterIsInstance<SongItem>().forEach { song ->
                                        XevSongRow(
                                            title        = song.title,
                                            artists      = song.artists?.joinToString { it.name } ?: "",
                                            thumbnail    = song.thumbnail,
                                            isPlaying    = isPlaying && mediaMetadata?.id == song.id,
                                            isActive     = mediaMetadata?.id == song.id,
                                            onPlay       = {
                                                if (!isGuest) {
                                                    if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                                    else playerConnection.playQueue(
                                                        YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                                                    )
                                                }
                                            },
                                            onLongClick  = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                }
                                            },
                                            onAddToQueue = {
                                                playerConnection.player.addMediaItem(song.toMediaMetadata().toMediaItem())
                                            },
                                            modifier     = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        } else {
                            // Exact Xevrae horizontal scroll section
                            AnimatedVisibility(section.items.isNotEmpty()) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier          = Modifier.padding(horizontal = 20.dp),
                                    ) {
                                        Text(
                                            text     = title,
                                            style    = MaterialTheme.typography.labelMedium,
                                            color    = Color.White,
                                            modifier = Modifier.weight(1f),
                                        )
                                        section.moreEndpoint?.let { ep ->
                                            TextButton(
                                                onClick = {
                                                    navController.navigate(
                                                        "artist/${viewModel.artistId}/items?browseId=${ep.browseId}?params=${ep.params}"
                                                    )
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                            ) { Text("More", style = MaterialTheme.typography.bodySmall) }
                                        }
                                    }
                                    LazyRow(verticalAlignment = Alignment.CenterVertically) {
                                        item { Spacer(Modifier.size(10.dp)) }
                                        items(section.items.distinctBy { it.id }) { item ->
                                            when (item) {
                                                is AlbumItem -> XevContentItem(
                                                    thumbnail   = item.thumbnail,
                                                    title       = item.title,
                                                    subtitle    = item.artists?.joinToString { it.name } ?: "",
                                                    onClick     = { navController.navigate("album/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                    thumbSize   = 180.dp,
                                                )
                                                is PlaylistItem -> XevContentItem(
                                                    thumbnail   = item.thumbnail ?: "",
                                                    title       = item.title,
                                                    subtitle    = item.author?.name ?: "",
                                                    onClick     = { navController.navigate("online_playlist/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                    thumbSize   = 180.dp,
                                                )
                                                is ArtistItem -> XevArtistItem(
                                                    thumbnail   = item.thumbnail,
                                                    title       = item.title,
                                                    subscribers = "",
                                                    onClick     = { navController.navigate("artist/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                )
                                                is SongItem -> XevContentItem(
                                                    thumbnail   = item.thumbnail,
                                                    title       = item.title,
                                                    subtitle    = item.artists?.joinToString { it.name } ?: "",
                                                    onClick     = {
                                                        if (!isGuest) playerConnection.playQueue(
                                                            YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata())
                                                        )
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                    thumbSize   = 180.dp,
                                                )
                                                else -> {}
                                            }
                                        }
                                        item { Spacer(Modifier.size(10.dp)) }
                                    }
                                }
                            }
                        }
                    }

                    // ── Description (exact Xevrae: label + ElevatedCard with palette color) ──
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text     = "Description",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = Color.White,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        )
                    }
                    ElevatedCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = CardDefaults.elevatedCardColors(
                            containerColor = paletteColor.rgbFactor(0.5f),
                        ),
                    ) {
                        val desc = page.description
                        ExpandableText(
                            text             = if (desc.isNullOrEmpty()) "No description" else desc,
                            collapsedMaxLines = 5,
                            modifier         = Modifier.padding(16.dp),
                        )
                    }

                    // Xevrae: EndOfPage()
                    XevEndOfPage()
                }
            }
        }
    }
}

// ── CollapsingToolbarParallaxEffect (exact Xevrae) ───────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XevCollapsingToolbar(
    title    : String,
    imageUrl : String?,
    onBack   : () -> Unit,
    content  : @Composable (color: Color) -> Unit,
) {
    val density       = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scroll        = rememberScrollState()

    val toolbarHeight   = TopAppBarDefaults.TopAppBarExpandedHeight +
        with(density) { WindowInsets.statusBars.getTop(this).toDp() * 2 }
    val headerHeight    = (configuration.screenHeightDp.dp * 2 / 4).coerceAtLeast(250.dp)
    val headerHeightPx  = with(density) { headerHeight.toPx() }
    val toolbarHeightPx = with(density) { toolbarHeight.toPx() }

    var paletteColor   by remember { mutableStateOf(Color.Black) }
    var showBackButton by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Parallax image header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .graphicsLayer {
                    translationY = -scroll.value.toFloat() / 2f
                    alpha        = (-1f / headerHeightPx) * scroll.value + 1
                }
                .background(paletteColor.rgbFactor(0.5f)),
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                onSuccess          = { state ->
                    val bmp = state.result.image.toBitmap()
                    Palette.from(bmp).maximumColorCount(8).generate { palette ->
                        if (palette != null) {
                            val raw = palette.getDominantColor(0xFF000000.toInt())
                            val r   = ((raw shr 16 and 0xFF) * 0.3f) / 255f
                            val g   = ((raw shr  8 and 0xFF) * 0.3f) / 255f
                            val b   = ((raw        and 0xFF) * 0.3f) / 255f
                            paletteColor = Color(r, g, b, 1f)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            // YouTube Music style gradient (exact Xevrae values)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black,
                            ),
                            startY = headerHeightPx / 2f,
                            endY   = headerHeightPx,
                        ),
                    ),
            )
        }

        // Scrollable body
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxSize().verticalScroll(scroll),
        ) {
            Spacer(Modifier.height(headerHeight))
            Box(Modifier.background(Color.Black)) {
                content(paletteColor)
            }
        }

        // Toolbar fades in when scrolled (exact Xevrae)
        val showToolbar by remember {
            derivedStateOf { scroll.value >= headerHeightPx - toolbarHeightPx }
        }
        LaunchedEffect(showToolbar) { showBackButton = !showToolbar }

        AnimatedVisibility(
            visible = showToolbar,
            enter   = fadeIn(tween(300)),
            exit    = fadeOut(tween(300)),
        ) {
            TopAppBar(
                windowInsets   = TopAppBarDefaults.windowInsets.exclude(
                    TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start)
                ),
                modifier       = Modifier.background(
                    Brush.verticalGradient(
                        listOf(paletteColor.rgbFactor(0.5f), paletteColor.rgbFactor(0.3f))
                    )
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).size(24.dp)) {
                        Icon(Icons.Default.ArrowBackIosNew, null, tint = Color.White)
                    }
                },
                title  = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }

        // Title animates from bottom of header into toolbar (exact Xevrae Title composable)
        var titleHeightPx by remember { mutableFloatStateOf(0f) }
        var titleWidthPx  by remember { mutableFloatStateOf(0f) }
        Text(
            text       = title,
            fontSize   = 30.sp,
            color      = Color.White,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier
                .graphicsLayer {
                    val collapseRange    = headerHeightPx - toolbarHeightPx
                    val collapseFraction = (scroll.value / collapseRange).coerceIn(0f, 1f)
                    val scaleXY          = lerp(TITLE_FONT_SCALE_START.dp, TITLE_FONT_SCALE_END.dp, collapseFraction)
                    val extraPad         = titleWidthPx.toDp() * (1 - scaleXY.value) / 2f
                    val titleY           = lerp(
                        lerp(headerHeight - titleHeightPx.toDp(), headerHeight / 2, collapseFraction),
                        lerp(headerHeight / 2, toolbarHeight / 2 - titleHeightPx.toDp() / 2, collapseFraction),
                        collapseFraction,
                    )
                    val titleX           = lerp(
                        lerp(TITLE_PADDING_START, (TITLE_PADDING_END - extraPad) * 5 / 4, collapseFraction),
                        lerp((TITLE_PADDING_END - extraPad) * 5 / 4, TITLE_PADDING_END - extraPad, collapseFraction),
                        collapseFraction,
                    )
                    translationY = titleY.toPx()
                    translationX = titleX.toPx()
                    scaleX       = scaleXY.value
                    scaleY       = scaleXY.value
                }
                .onGloballyPositioned {
                    titleHeightPx = it.size.height.toFloat()
                    titleWidthPx  = it.size.width.toFloat()
                },
        )

        // Back button visible when toolbar is hidden (exact Xevrae AnimatedVisibility)
        AnimatedVisibility(
            visible = showBackButton,
            enter   = fadeIn() + slideInHorizontally(),
            exit    = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .wrapContentSize()
                    .align(Alignment.TopStart)
                    .padding(top = with(density) { WindowInsets.statusBars.getTop(this).toDp() })
                    .padding(12.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    colors  = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.8f),
                        contentColor   = Color.White.copy(alpha = 0.6f),
                    ),
                ) { Icon(Icons.Default.ArrowBackIosNew, "Back") }
            }
        }
    }
}

// ── Animated border Follow button (Xevrae's LimitedBorderAnimationView) ───────
@Composable
private fun XevAnimatedBorderButton(
    isAnimated : Boolean,
    onClick    : () -> Unit,
    content    : @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "borderAnim")
    val alpha by transition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label         = "borderAlpha",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        OutlinedButton(
            onClick = onClick,
            border  = BorderStroke(
                width = 2.dp,
                brush = if (isAnimated) {
                    Brush.sweepGradient(
                        listOf(Color.Gray.copy(alpha), Color.White.copy(alpha), Color.Gray.copy(alpha))
                    )
                } else {
                    Brush.linearGradient(listOf(Color.Gray.copy(0.5f), Color.Gray.copy(0.5f)))
                },
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor   = Color.White,
                containerColor = Color.Transparent,
            ),
        ) { content() }
    }
}

// ── SongFullWidthItems port with swipe-to-queue (exact Xevrae) ────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XevSongRow(
    title        : String,
    artists      : String,
    thumbnail    : String,
    isPlaying    : Boolean,
    isActive     : Boolean,
    onPlay       : () -> Unit,
    onLongClick  : () -> Unit,
    onAddToQueue : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density        = LocalDensity.current
    val maxOffset      = 360f
    val offsetX        = remember { Animatable(0f) }
    var heightDp       by remember { mutableStateOf(0.dp) }

    Box(modifier = modifier) {
        // Queue icon revealed behind (Xevrae: Crossfade)
        Crossfade(offsetX.value >= maxOffset / 2, label = "swipe") { showQueue ->
            if (showQueue) {
                Box(
                    modifier = Modifier
                        .height(heightDp)
                        .padding(start = 15.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue", tint = Color.White)
                }
            }
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
                .animateContentSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, drag ->
                            if (offsetX.value + drag > 0) {
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo((offsetX.value + drag).coerceAtMost(maxOffset))
                                }
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
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    Icon(painterResource(R.drawable.pause), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(thumbnail).crossfade(true)
                .allowHardware(false)
                .build(),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                        .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable()
                        .padding(bottom = 3.dp),
                )
                Text(
                    text     = artists,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color    = Color(0xC4FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                        .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
                )
            }
            IconButton(onClick = onLongClick) {
                Icon(painterResource(R.drawable.more_vert), null, tint = Color.White)
            }
        }
    }
}

// ── HomeItemContentPlaylist port (albums, singles, featured on, playlists) ────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XevContentItem(
    thumbnail   : String,
    title       : String,
    subtitle    : String,
    onClick     : () -> Unit,
    onLongClick : () -> Unit = {},
    thumbSize   : Dp = 180.dp,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .wrapContentSize()
            .focusable(true)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(Modifier.padding(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(thumbnail).crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(thumbSize)
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
                    .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .width(thumbSize)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
        }
    }
}

// ── HomeItemArtist port (related artists — circle thumbnail) ──────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XevArtistItem(
    thumbnail   : String?,
    title       : String,
    subscribers : String,
    onClick     : () -> Unit,
    onLongClick : () -> Unit = {},
    thumbSize   : Dp = 160.dp,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(Modifier.padding(10.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(thumbnail).crossfade(true)
                .allowHardware(false)
                .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(thumbSize)
                    .clip(CircleShape),
            )
            Text(
                text      = title,
                style     = MaterialTheme.typography.titleSmall,
                color     = Color.White,
                maxLines  = 1,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .width(thumbSize)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(top = 8.dp)
                    .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            Text(
                text      = subscribers.ifEmpty { "Artist" },
                style     = MaterialTheme.typography.bodySmall,
                maxLines  = 1,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .width(thumbSize)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .basicMarquee(Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                    .focusable(),
            )
            // Exact Xevrae: blank third row spacer
            Text(
                text  = "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(thumbSize).wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

// ── EndOfPage (Xevrae: 280dp box with version text) ───────────────────────────
@Composable
private fun XevEndOfPage() {
    Box(
        modifier         = Modifier.fillMaxWidth().height(280.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text      = "© 2026 Dare ${com.dare.music.BuildConfig.VERSION_NAME}\nt4ulquiorra",
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color     = Color.Gray,
            modifier  = Modifier.padding(top = 20.dp).alpha(0.8f),
        )
    }
}
