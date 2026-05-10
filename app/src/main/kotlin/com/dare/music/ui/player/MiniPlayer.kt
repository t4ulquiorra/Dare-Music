/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MiniPlayer — ported 1:1 from Xevrae Android MiniPlayer
 */

package com.dare.music.ui.player

import android.graphics.Bitmap as AndroidBitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.dare.music.constants.MiniPlayerBackgroundStyle
import com.dare.music.constants.MiniPlayerBackgroundStyleKey
import com.dare.music.constants.MiniPlayerHeight
import com.dare.music.extensions.togglePlayPause
import com.dare.music.utils.rememberEnumPreference
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.effect
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
    val density = LocalDensity.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    // ── Glass preference — maps to Xevrae's isLiquidGlassEnabled ─────────
    // backdrop!=null is always true in Dare; must read the actual pref
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val isLiquidGlassEnabled =
        miniPlayerBackground == MiniPlayerBackgroundStyle.TRANSPARENT && backdrop != null

    // ── Player state ──────────────────────────────────────────────────────
    val mediaMetadata   by playerConnection.mediaMetadata.collectAsState()
    val isPlaying       by playerConnection.isPlaying.collectAsState()
    val playbackState   by playerConnection.playbackState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext     by playerConnection.canSkipNext.collectAsState()

    val isLoading = playbackState == Player.STATE_BUFFERING

    // ── Liked state ───────────────────────────────────────────────────────
    val songId      = mediaMetadata?.id ?: ""
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isLiked     = librarySong?.song?.liked == true

    // ── Palette-extracted background ──────────────────────────────────────
    val background = remember { ColorAnimatable(Color.DarkGray) }

    // Text colour from palette luminance (Xevrae samples layer pixels;
    // we derive it from the palette colour — same effect without the layer)
    val luminance = remember(background.value) {
        val c = background.value
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    }
    val textColor by animateColorAsState(
        targetValue    = if (isLiquidGlassEnabled || luminance <= 0.6f) Color.White else Color.Black,
        animationSpec  = tween(500),
        label          = "MiniPlayerTextColor",
    )

    // ── Progress ──────────────────────────────────────────────────────────
    val progress = remember(positionState.longValue, durationState.longValue) {
        val dur = durationState.longValue
        if (dur > 0L && positionState.longValue >= 0L)
            positionState.longValue.toFloat() / dur.toFloat()
        else 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label         = "MiniPlayerProgress",
    )

    // ── Swipe offsets (exact Xevrae structure) ────────────────────────────
    // offsetY on the Card  → vertical dismiss drag
    // offsetX on the inner content Box → horizontal prev/next drag
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // ── Card (shape + colour mirror Xevrae exactly) ───────────────────────
    val cardShape = if (isLiquidGlassEnabled) CircleShape else RoundedCornerShape(12.dp)
    val cardColor = if (isLiquidGlassEnabled) Color.Transparent else background.value

    Card(
        shape  = cardShape,
        colors = CardDefaults.cardColors(
            containerColor         = cardColor,
            disabledContainerColor = cardColor,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            // Glass modifier — Xevrae: drawBackdropCustomShape; Dare: drawBackdrop + shader
            .then(
                if (isLiquidGlassEnabled && backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop      = backdrop,
                        shape         = { RoundedCornerShape(16.dp) },
                        effects       = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val refractionHeightPx = with(density) { 20.dp.toPx() }
                                val refractionAmountPx = with(density) { 67.dp.toPx() }
                                val shaderString = """
                                    uniform shader content;
                                    uniform float2 size;
                                    uniform float2 offset;
                                    uniform float4 cornerRadii;
                                    uniform float refractionHeight;
                                    uniform float refractionAmount;
                                    uniform float depthEffect;
                                    uniform float chromaticAberration;
                                    float radiusAt(float2 coord, float4 radii) {
                                        if (coord.x >= 0.0) { if (coord.y <= 0.0) return radii.y; else return radii.z; }
                                        else { if (coord.y <= 0.0) return radii.x; else return radii.w; }
                                    }
                                    float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
                                        float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                                        float outside = length(max(cornerCoord, 0.0)) - radius;
                                        float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
                                        return outside + inside;
                                    }
                                    float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
                                        float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                                        if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) { return sign(coord) * normalize(max(cornerCoord, 0.0)); }
                                        else { float gradX = step(cornerCoord.y, cornerCoord.x); return sign(coord) * float2(gradX, 1.0 - gradX); }
                                    }
                                    float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }
                                    half4 main(float2 coord) {
                                        float2 halfSize = size * 0.5;
                                        float2 centeredCoord = (coord + offset) - halfSize;
                                        float radius = radiusAt(coord, cornerRadii);
                                        float sd = sdRoundedRect(centeredCoord, halfSize, radius);
                                        if (-sd >= refractionHeight) { return content.eval(coord); }
                                        sd = min(sd, 0.0);
                                        float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
                                        float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
                                        float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
                                        float2 refractedCoord = coord + d * grad;
                                        float dispersionAmount = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
                                        float2 dispersedCoord = d * grad * dispersionAmount;
                                        half4 color = half4(0.0);
                                        half4 red = content.eval(refractedCoord + dispersedCoord); color.r += red.r / 3.5; color.a += red.a / 7.0;
                                        half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0)); color.r += orange.r / 3.5; color.g += orange.g / 7.0; color.a += orange.a / 7.0;
                                        half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0)); color.r += yellow.r / 3.5; color.g += yellow.g / 3.5; color.a += yellow.a / 7.0;
                                        half4 green = content.eval(refractedCoord); color.g += green.g / 3.5; color.a += green.a / 7.0;
                                        half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0)); color.g += cyan.g / 3.5; color.b += cyan.b / 3.0; color.a += cyan.a / 7.0;
                                        half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0)); color.b += blue.b / 3.0; color.a += blue.a / 7.0;
                                        half4 purple = content.eval(refractedCoord - dispersedCoord); color.r += purple.r / 7.0; color.b += purple.b / 3.0; color.a += purple.a / 7.0;
                                        return color;
                                    }
                                """.trimIndent()
                                val shader = RuntimeShader(shaderString)
                                val cornerRadiusPx = with(density) { 16.dp.toPx() }
                                shader.setFloatUniform("size", size.width, size.height)
                                shader.setFloatUniform("offset", -padding, -padding)
                                shader.setFloatUniform("cornerRadii", floatArrayOf(cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx))
                                shader.setFloatUniform("refractionHeight", refractionHeightPx)
                                shader.setFloatUniform("refractionAmount", -refractionAmountPx)
                                shader.setFloatUniform("depthEffect", 1.0f)
                                shader.setFloatUniform("chromaticAberration", 0.0f)
                                effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
                            }
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) },
                    )
                } else Modifier,
            )
            .clipToBounds()
            // offsetY on card — vertical dismiss (Xevrae: onClose after >70px)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {},
                    onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                        if (offsetY.value + dragAmount > 0) {
                            coroutineScope.launch {
                                change.consume()
                                offsetY.animateTo(offsetY.value + 2 * dragAmount)
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch { offsetY.animateTo(0f) }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value > 70) {
                                playerConnection.player.stop()
                                playerConnection.player.clearMediaItems()
                            }
                            offsetY.animateTo(0f)
                        }
                    },
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.size(8.dp))

                // ── Inner Box — offsetX + horizontal drag (exact Xevrae structure) ──
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = {},
                                    onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        coroutineScope.launch {
                                            change.consume()
                                            offsetX.animateTo(offsetX.value + dragAmount * 2)
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            if (offsetX.value > 200)       playerConnection.seekToPrevious()
                                            else if (offsetX.value < -120) playerConnection.seekToNext()
                                            offsetX.animateTo(0f)
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (offsetX.value > 200)       playerConnection.seekToPrevious()
                                            else if (offsetX.value < -120) playerConnection.seekToNext()
                                            offsetX.animateTo(0f)
                                        }
                                    },
                                )
                            },
                    ) {
                        // Album art — also drives palette extraction
                        AsyncImage(
                            model              = ImageRequest.Builder(context)
                                .data(mediaMetadata?.thumbnailUrl)
                                .build(),
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            onSuccess          = { state ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    val bmp: AndroidBitmap = state.result.image.toBitmap()
                                    val palette = Palette.from(bmp).generate()
                                    val argb    = palette.getDominantColor(Color.DarkGray.toArgb())
                                    withContext(Dispatchers.Main) {
                                        background.animateTo(Color(argb), tween(500))
                                    }
                                }
                            },
                            modifier           = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(4.dp)),
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Song info with slide transitions on track change
                        AnimatedContent(
                            targetState    = mediaMetadata,
                            modifier       = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                            transitionSpec = {
                                if (targetState != initialState) {
                                    (slideInHorizontally { width -> width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> +width } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> +width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
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
                                    Text(
                                        text     = meta.title,
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = textColor,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations    = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            )
                                            .focusable(),
                                    )
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
                        contentDescription = null,
                        tint               = if (isLiked) Color(0xFFE53935) else textColor,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))

                // Play/Pause — crossfades to spinner while buffering
                Crossfade(targetState = isLoading, label = "MiniPlayerPlayState") { loading ->
                    if (loading) {
                        Box(
                            modifier         = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.LightGray,
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
                                contentDescription = null,
                                tint               = textColor,
                                modifier           = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(15.dp))
            }

            // Progress bar — 1dp line at bottom edge (exact Xevrae layout)
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 10.dp)
                    .align(Alignment.BottomCenter),
            ) {
                LinearProgressIndicator(
                    progress          = { animatedProgress },
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Transparent, RoundedCornerShape(4.dp)),
                    color             = Color.White,
                    trackColor        = Color.Transparent,
                    strokeCap         = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
