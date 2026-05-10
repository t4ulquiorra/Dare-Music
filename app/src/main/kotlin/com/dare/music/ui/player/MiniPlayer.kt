/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MiniPlayer — visual design ported from Xevrae
 */

package com.dare.music.ui.player

import android.graphics.Bitmap as AndroidBitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap
import com.dare.music.LocalDatabase
import com.dare.music.LocalPlayerConnection
import com.dare.music.R
import com.dare.music.constants.MiniPlayerHeight
import com.dare.music.extensions.togglePlayPause
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.dare.music.ui.component.Icon as MIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val isGlassEnabled = backdrop != null

    // ── Player state ──────────────────────────────────────────────────────
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying     by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext     by playerConnection.canSkipNext.collectAsState()

    // ── Liked state from DB ───────────────────────────────────────────────
    val songId      = mediaMetadata?.id ?: ""
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isLiked     = librarySong?.song?.liked == true

    // Treat buffering as "loading" (shows spinner instead of play/pause)
    val isLoading = playbackState == Player.STATE_BUFFERING

    // ── Palette-extracted card background ─────────────────────────────────
    val background = remember { ColorAnimatable(Color.DarkGray) }

    // Derive text color from relative luminance so it always contrasts the background
    val luminance = remember(background.value) {
        val c = background.value
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    }
    val textColor by animateColorAsState(
        targetValue = if (isGlassEnabled || luminance <= 0.6f) Color.White else Color.Black,
        animationSpec = tween(500),
        label = "MiniPlayerTextColor",
    )

    // ── Progress 0f..1f ───────────────────────────────────────────────────
    val progress = remember(positionState.longValue, durationState.longValue) {
        val dur = durationState.longValue
        if (dur > 0L && positionState.longValue >= 0L)
            positionState.longValue.toFloat() / dur.toFloat()
        else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "MiniPlayerProgress",
    )

    // ── Horizontal swipe offset (prev / next) ─────────────────────────────
    val offsetX = remember { Animatable(0f) }

    // ── Root card ─────────────────────────────────────────────────────────
    Card(
        shape = if (isGlassEnabled) CircleShape else RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor         = if (isGlassEnabled) Color.Transparent else background.value,
            disabledContainerColor = if (isGlassEnabled) Color.Transparent else background.value,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .then(
                // Liquid-glass backdrop when available; plain palette colour otherwise
                backdrop?.let { bd ->
                    Modifier.drawBackdrop(
                        backdrop      = bd,
                        shape         = { RoundedCornerShape(16.dp) },
                        effects       = {},
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) },
                    )
                } ?: Modifier,
            )
            .clipToBounds()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        coroutineScope.launch {
                            change.consume()
                            offsetX.animateTo(offsetX.value + dragAmount * 2)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch { offsetX.animateTo(0f) }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            when {
                                offsetX.value >  200f && canSkipPrevious -> playerConnection.seekToPrevious()
                                offsetX.value < -120f && canSkipNext     -> playerConnection.seekToNext()
                            }
                            offsetX.animateTo(0f)
                        }
                    },
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {

            // ── Main content row ──────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            ) {
                Spacer(modifier = Modifier.size(8.dp))

                // Album-art thumbnail — also drives palette extraction
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp: AndroidBitmap = state.result.image.toBitmap()
                            val palette = Palette.from(bmp).generate()
                            val argb = palette.getDominantColor(Color.DarkGray.toArgb())
                            withContext(Dispatchers.Main) {
                                background.animateTo(Color(argb), tween(500))
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(4.dp)),
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Song info — slides in/out horizontally on track changes
                AnimatedContent(
                    targetState = mediaMetadata,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                    transitionSpec = {
                        if (targetState != initialState) {
                            (slideInHorizontally { it } + fadeIn())
                                .togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn())
                                .togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "MiniPlayerSongInfo",
                ) { meta ->
                    if (meta != null) {
                        Column(
                            modifier = Modifier
                                .wrapContentHeight()
                                .align(Alignment.CenterVertically),
                        ) {
                            // Title with auto-scrolling marquee
                            Text(
                                text     = meta.title,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .basicMarquee(
                                        iterations    = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    )
                                    .focusable(),
                            )
                            // Artist row — explicit badge + artist name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(visible = meta.explicit) {
                                    MIcon.Explicit()
                                }
                                if (meta.artists.any { it.name.isNotBlank() }) {
                                    Text(
                                        text     = meta.artists.joinToString { it.name },
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = textColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations    = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            )
                                            .focusable(),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(15.dp))

                // Heart / like button
                IconButton(
                    onClick  = { if (songId.isNotEmpty()) playerConnection.service.toggleLike() },
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        painter            = painterResource(
                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                        ),
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint               = if (isLiked) Color(0xFFE53935) else textColor,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))

                // Play/Pause — crossfades to spinner while buffering
                Crossfade(
                    targetState = isLoading,
                    label       = "MiniPlayerLoadState",
                ) { loading ->
                    if (loading) {
                        Box(
                            modifier          = Modifier.size(48.dp),
                            contentAlignment  = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = textColor,
                                strokeWidth = 3.dp,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.togglePlayPause()
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                painter            = painterResource(
                                    if (isPlaying) R.drawable.pause else R.drawable.play,
                                ),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint               = textColor,
                                modifier           = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(15.dp))
            }

            // ── Progress bar — 1 dp line pinned to the bottom edge ────────
            LinearProgressIndicator(
                progress         = { animatedProgress },
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 10.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Transparent, RoundedCornerShape(4.dp)),
                color            = Color.White,
                trackColor       = Color.Transparent,
                strokeCap        = StrokeCap.Round,
                drawStopIndicator = {},
            )
        }
    }
}
