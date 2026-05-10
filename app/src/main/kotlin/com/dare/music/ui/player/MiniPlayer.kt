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
import androidx.compose.foundation.shape.CircleShape
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

    // ── Style preference (maps to Xevrae's isLiquidGlassEnabled) ─────────
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    // Glass = TRANSPARENT style with a live backdrop available
    val isGlass = miniPlayerBackground == MiniPlayerBackgroundStyle.TRANSPARENT && backdrop != null

    // ── Player state ──────────────────────────────────────────────────────
    val mediaMetadata   by playerConnection.mediaMetadata.collectAsState()
    val isPlaying       by playerConnection.isPlaying.collectAsState()
    val playbackState   by playerConnection.playbackState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext     by playerConnection.canSkipNext.collectAsState()
    val isLoading        = playbackState == Player.STATE_BUFFERING

    // ── Liked state ───────────────────────────────────────────────────────
    val songId      = mediaMetadata?.id ?: ""
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isLiked     = librarySong?.song?.liked == true

    // ── Palette background (animates when album art loads) ────────────────
    val background = remember { ColorAnimatable(Color(0xFF1E1E1E)) }

    // background colour fed into drawBackdrop's onDrawSurface
    val backgroundColor = when (miniPlayerBackground) {
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Transparent
        MiniPlayerBackgroundStyle.PURE_BLACK  -> Color.Black
        else -> background.value   // DEFAULT / BLUR / GRADIENT → palette colour
    }

    // ── Text colour from luminance ────────────────────────────────────────
    val luminance = remember(background.value) {
        val c = background.value
        0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    }
    val textColor by animateColorAsState(
        targetValue   = if (isGlass || luminance <= 0.6f) Color.White else Color.Black,
        animationSpec = tween(500),
        label         = "MiniPlayerTextColor",
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

    // ── Shape ─────────────────────────────────────────────────────────────
    // Xevrae: CircleShape when liquid glass, RoundedCornerShape(12) otherwise
    val shape = if (isGlass) CircleShape else RoundedCornerShape(12.dp)

    // ── Horizontal swipe offset (prev / next) ─────────────────────────────
    val offsetX = remember { Animatable(0f) }

    // ── Outer wrapper ─────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight),
    ) {
        // ── Background box: clip + drawBackdrop (always when backdrop≠null) ──
        // KEY FIX: In Dare's backdrop system, drawBackdrop must be used
        // whenever backdrop≠null — even for non-glass styles — because the
        // backdrop layer owns the compositing surface. The style only changes
        // what onDrawSurface draws (solid palette colour vs frosted glass).
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
                                // Refraction shader only for glass + Android 13+
                                if (isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val refractionHeightPx = with(density) { 20.dp.toPx() }
                                    val refractionAmountPx = with(density) { 67.dp.toPx() }
                                    val cornerRadiusPx     = with(density) { 12.dp.toPx() }
                                    val shaderSrc = """
                                        uniform shader content;
                                        uniform float2 size;
                                        uniform float2 offset;
                                        uniform float4 cornerRadii;
                                        uniform float refractionHeight;
                                        uniform float refractionAmount;
                                        uniform float depthEffect;
                                        uniform float chromaticAberration;
                                        float radiusAt(float2 c,float4 r){if(c.x>=0.0){if(c.y<=0.0)return r.y;else return r.z;}else{if(c.y<=0.0)return r.x;else return r.w;}}
                                        float sdRR(float2 c,float2 h,float r){float2 q=abs(c)-(h-float2(r));float o=length(max(q,0.0))-r;float i=min(max(q.x,q.y),0.0);return o+i;}
                                        float2 gradSdRR(float2 c,float2 h,float r){float2 q=abs(c)-(h-float2(r));if(q.x>=0.0||q.y>=0.0){return sign(c)*normalize(max(q,0.0));}else{float gx=step(q.y,q.x);return sign(c)*float2(gx,1.0-gx);}}
                                        float circleMap(float x){return 1.0-sqrt(1.0-x*x);}
                                        half4 main(float2 coord){
                                            float2 h=size*0.5;float2 cc=(coord+offset)-h;
                                            float radius=radiusAt(coord,cornerRadii);
                                            float sd=sdRR(cc,h,radius);
                                            if(-sd>=refractionHeight){return content.eval(coord);}
                                            sd=min(sd,0.0);
                                            float d=circleMap(1.0--sd/refractionHeight)*refractionAmount;
                                            float gr=min(radius*1.5,min(h.x,h.y));
                                            float2 grad=normalize(gradSdRR(cc,h,gr)+depthEffect*normalize(cc));
                                            float2 rc=coord+d*grad;
                                            half4 color=half4(0.0);
                                            half4 g=content.eval(rc);color.g+=g.g/3.5;color.a+=g.a/7.0;
                                            half4 b=content.eval(rc);color.b+=b.b/3.0;color.a+=b.a/7.0;
                                            half4 pu=content.eval(rc);color.r+=pu.r/7.0;color.b+=pu.b/3.0;color.a+=pu.a/7.0;
                                            return color;}
                                    """.trimIndent()
                                    val shader = RuntimeShader(shaderSrc)
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
                            onDrawSurface = {
                                // Glass → frosted white tint; everything else → solid palette/black
                                if (isGlass) drawRect(Color.White.copy(alpha = 0.12f))
                                else         drawRect(backgroundColor)
                            },
                        )
                    } ?: Modifier.background(backgroundColor) // no backdrop → plain colour
                )
                .border(
                    width  = 1.dp,
                    color  = Color.White.copy(alpha = if (isGlass) 0.25f else 0.10f),
                    shape  = shape,
                ),
        ) {
            // ── Content row ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.size(8.dp))

                // Inner Box — offsetX + horizontal drag (Xevrae exact structure)
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart  = {},
                                    onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        coroutineScope.launch {
                                            change.consume()
                                            offsetX.animateTo(offsetX.value + dragAmount * 2)
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            if (offsetX.value > 200)        playerConnection.seekToPrevious()
                                            else if (offsetX.value < -120)  playerConnection.seekToNext()
                                            offsetX.animateTo(0f)
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (offsetX.value > 200)        playerConnection.seekToPrevious()
                                            else if (offsetX.value < -120)  playerConnection.seekToNext()
                                            offsetX.animateTo(0f)
                                        }
                                    },
                                )
                            },
                    ) {
                        // Album art — also triggers palette extraction
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

                        // Song info — slides on track change (Xevrae exact transition)
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

                // Heart / like
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

                // Play/Pause or buffering spinner
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

            // Progress bar — 1dp at bottom edge (Xevrae exact layout)
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
