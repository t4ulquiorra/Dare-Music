/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * MiniPlayer — ported from Xevrae Android MiniPlayer
 */

package com.dare.music.ui.player

import android.graphics.Bitmap as AndroidBitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil3.request.allowHardware
import coil3.toBitmap
import com.dare.music.LocalDatabase
import com.dare.music.LocalPlayerConnection
import com.dare.music.R
import com.dare.music.constants.MiniPlayerHeight
import com.dare.music.constants.MiniPlayerLiquidGlassKey
import com.dare.music.extensions.togglePlayPause
import com.dare.music.utils.rememberPreference
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
    val context          = LocalContext.current
    val density          = LocalDensity.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database         = LocalDatabase.current
    val coroutineScope   = rememberCoroutineScope()

    val liquidGlassEnabled by rememberPreference(MiniPlayerLiquidGlassKey, defaultValue = false)
    val isGlass = liquidGlassEnabled && backdrop != null

    val mediaMetadata   by playerConnection.mediaMetadata.collectAsState()
    val isPlaying       by playerConnection.isPlaying.collectAsState()
    val playbackState   by playerConnection.playbackState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext     by playerConnection.canSkipNext.collectAsState()
    val isLoading        = playbackState == Player.STATE_BUFFERING

    val songId      = mediaMetadata?.id ?: ""
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isLiked     = librarySong?.song?.liked == true

    val background = remember { ColorAnimatable(Color(0xFF1E1E1E)) }

    val luminance = remember(background.value) {
        val c = background.value
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    }
    val textColor by animateColorAsState(
        targetValue   = if (isGlass || luminance <= 0.6f) Color.White else Color.Black,
        animationSpec = tween(500),
        label         = "MiniPlayerTextColor",
    )

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

    val shape  = RoundedCornerShape(12.dp)
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(
                    backdrop?.let { bd ->
                        Modifier.drawBackdrop(
                            backdrop      = bd,
                            shape         = { shape },
                            effects       = {
                                if (isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val refractionHeightPx = with(density) { 20.dp.toPx() }
                                    val refractionAmountPx = with(density) { 67.dp.toPx() }
                                    val cornerRadiusPx     = with(density) { 12.dp.toPx() }
                                    val shaderSource = """
                                        uniform shader content;
                                        uniform float2 size;
                                        uniform float2 offset;
                                        uniform float4 cornerRadii;
                                        uniform float refractionHeight;
                                        uniform float refractionAmount;
                                        uniform float depthEffect;
                                        uniform float chromaticAberration;

                                        float radiusAt(float2 coord, float4 radii) {
                                            if (coord.x >= 0.0) {
                                                if (coord.y <= 0.0) return radii.y;
                                                else return radii.z;
                                            } else {
                                                if (coord.y <= 0.0) return radii.x;
                                                else return radii.w;
                                            }
                                        }

                                        float signedDistRoundedRect(float2 coord, float2 halfSize, float radius) {
                                            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                                            float outsideDist = length(max(cornerCoord, 0.0)) - radius;
                                            float insideDist  = min(max(cornerCoord.x, cornerCoord.y), 0.0);
                                            return outsideDist + insideDist;
                                        }

                                        float2 gradSignedDistRoundedRect(float2 coord, float2 halfSize, float radius) {
                                            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                                            if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
                                                return sign(coord) * normalize(max(cornerCoord, 0.0));
                                            } else {
                                                float gradX = step(cornerCoord.y, cornerCoord.x);
                                                return sign(coord) * float2(gradX, 1.0 - gradX);
                                            }
                                        }

                                        float circleMap(float xVal) {
                                            return 1.0 - sqrt(1.0 - xVal * xVal);
                                        }

                                        half4 main(float2 coord) {
                                            float2 halfSize      = size * 0.5;
                                            float2 centeredCoord = (coord + offset) - halfSize;
                                            float  radius        = radiusAt(coord, cornerRadii);
                                            float  dist          = signedDistRoundedRect(centeredCoord, halfSize, radius);

                                            if (-dist >= refractionHeight) {
                                                return content.eval(coord);
                                            }

                                            dist = min(dist, 0.0);
                                            float  dispAmount = circleMap(1.0 - (-dist / refractionHeight)) * refractionAmount;
                                            float  gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
                                            float2 gradVec    = normalize(
                                                gradSignedDistRoundedRect(centeredCoord, halfSize, gradRadius)
                                                + depthEffect * normalize(centeredCoord)
                                            );
                                            float2 refractCoord = coord + dispAmount * gradVec;
                                            float  dispersion   = chromaticAberration
                                                * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
                                            float2 disperseVec  = dispAmount * gradVec * dispersion;

                                            half4 result = half4(0.0);
                                            half4 redSample    = content.eval(refractCoord + disperseVec);
                                            result.r += redSample.r / 3.5;
                                            result.a += redSample.a / 7.0;
                                            half4 orangeSample = content.eval(refractCoord + disperseVec * (2.0 / 3.0));
                                            result.r += orangeSample.r / 3.5;
                                            result.g += orangeSample.g / 7.0;
                                            result.a += orangeSample.a / 7.0;
                                            half4 yellowSample = content.eval(refractCoord + disperseVec * (1.0 / 3.0));
                                            result.r += yellowSample.r / 3.5;
                                            result.g += yellowSample.g / 3.5;
                                            result.a += yellowSample.a / 7.0;
                                            half4 greenSample  = content.eval(refractCoord);
                                            result.g += greenSample.g / 3.5;
                                            result.a += greenSample.a / 7.0;
                                            half4 cyanSample   = content.eval(refractCoord - disperseVec * (1.0 / 3.0));
                                            result.g += cyanSample.g / 3.5;
                                            result.b += cyanSample.b / 3.0;
                                            result.a += cyanSample.a / 7.0;
                                            half4 blueSample   = content.eval(refractCoord - disperseVec * (2.0 / 3.0));
                                            result.b += blueSample.b / 3.0;
                                            result.a += blueSample.a / 7.0;
                                            half4 purpleSample = content.eval(refractCoord - disperseVec);
                                            result.r += purpleSample.r / 7.0;
                                            result.b += purpleSample.b / 3.0;
                                            result.a += purpleSample.a / 7.0;
                                            return result;
                                        }
                                    """.trimIndent()
                                    val shader = RuntimeShader(shaderSource)
                                    shader.setFloatUniform("size", size.width, size.height)
                                    shader.setFloatUniform("offset", -padding, -padding)
                                    shader.setFloatUniform("cornerRadii", floatArrayOf(
                                        cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx,
                                    ))
                                    shader.setFloatUniform("refractionHeight", refractionHeightPx)
                                    shader.setFloatUniform("refractionAmount", -refractionAmountPx)
                                    shader.setFloatUniform("depthEffect", 1.0f)
                                    shader.setFloatUniform("chromaticAberration", 0.0f)
                                    val blurEffect = RenderEffect.createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                                    effect(RenderEffect.createChainEffect(shaderEffect, blurEffect))
                                }
                            },
                            onDrawSurface = {
                                if (isGlass) drawRect(Color.White.copy(alpha = 0.12f))
                                else         drawRect(background.value)
                            },
                        )
                    } ?: Modifier.background(background.value)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isGlass) 0.25f else 0.10f),
                    shape = shape,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.size(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart      = {},
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
                        AsyncImage(
                            model              = ImageRequest.Builder(context)
                                .data(mediaMetadata?.thumbnailUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            onSuccess          = { state ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    val bmp: AndroidBitmap = state.result.image.toBitmap()
                                    val palette = Palette.from(bmp).generate()
                                    val argb    = palette.getDominantColor(Color(0xFF1E1E1E).toArgb())
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

                        AnimatedContent(
                            targetState      = mediaMetadata,
                            modifier         = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                            transitionSpec   = {
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

                Crossfade(targetState = isLoading, label = "MiniPlayerPlayState") { loading ->
                    if (loading) {
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = Color.LightGray,
                                strokeWidth = 3.dp,
                            )
                        }
                    } else {
                        IconButton(
                            onClick  = {
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
