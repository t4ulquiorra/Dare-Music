#!/bin/bash
# Patch script: Add liquid glass backdrop to Dare's MiniPlayer
# Run from ~/Dare

set -e

MAIN="app/src/main/kotlin/com/dare/music/MainActivity.kt"
PLAYER="app/src/main/kotlin/com/dare/music/ui/player/Player.kt"
MINI="app/src/main/kotlin/com/dare/music/ui/player/MiniPlayer.kt"

python3 - <<'PYEOF'
import re

# ─── 1. MainActivity.kt ───────────────────────────────────────────────────────
path = "app/src/main/kotlin/com/dare/music/MainActivity.kt"
with open(path) as f:
    content = f.read()

# Add imports after the last com.dare import block
content = content.replace(
    "import com.dare.music.ui.player.BottomSheetPlayer",
    "import com.dare.music.ui.player.BottomSheetPlayer\nimport com.kyant.backdrop.backdrops.layerBackdrop\nimport com.kyant.backdrop.backdrops.rememberLayerBackdrop"
)

# Add glassBackdrop before the Scaffold/Box that contains content
# It's right before the first BottomSheetPlayer usage — find the CompositionLocalProvider closing brace area
# We add it right before the Box(Modifier.weight(1f)) line
content = content.replace(
    'Box(Modifier.weight(1f)) {',
    'val glassBackdrop = rememberLayerBackdrop()\n                            Box(Modifier.weight(1f).layerBackdrop(glassBackdrop)) {'
)

# Add backdrop param to both BottomSheetPlayer calls
content = content.replace(
    '''BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                        positionState = positionState,
                                        durationState = durationState,
                                        modifier = Modifier,
                                    )''',
    '''BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                        positionState = positionState,
                                        durationState = durationState,
                                        backdrop = glassBackdrop,
                                        modifier = Modifier,
                                    )'''
)
content = content.replace(
    '''BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                        positionState = positionState,
                                        durationState = durationState,
                                    )''',
    '''BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                        pureBlack = pureBlack,
                                        positionState = positionState,
                                        durationState = durationState,
                                        backdrop = glassBackdrop,
                                    )'''
)

with open(path, "w") as f:
    f.write(content)
print("✅ MainActivity.kt patched")

# ─── 2. Player.kt ─────────────────────────────────────────────────────────────
path = "app/src/main/kotlin/com/dare/music/ui/player/Player.kt"
with open(path) as f:
    content = f.read()

# Add Backdrop import
content = content.replace(
    "package com.dare.music.ui.player",
    "package com.dare.music.ui.player\n"
)
# Add import after existing imports block
content = content.replace(
    "import kotlinx.coroutines.launch",
    "import com.kyant.backdrop.Backdrop\nimport kotlinx.coroutines.launch"
)

# Add backdrop param to BottomSheetPlayer signature
content = content.replace(
    """fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    positionState: MutableLongState,
    durationState: MutableLongState,
)""",
    """fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    positionState: MutableLongState,
    durationState: MutableLongState,
    backdrop: Backdrop? = null,
)"""
)

# Pass backdrop to MiniPlayer call
content = content.replace(
    """MiniPlayer(
                positionState = positionState,
                durationState = durationState,
            )""",
    """MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                backdrop = backdrop,
            )"""
)

with open(path, "w") as f:
    f.write(content)
print("✅ Player.kt patched")

# ─── 3. MiniPlayer.kt ─────────────────────────────────────────────────────────
path = "app/src/main/kotlin/com/dare/music/ui/player/MiniPlayer.kt"
with open(path) as f:
    content = f.read()

# Add imports
content = content.replace(
    "import kotlinx.coroutines.launch",
    """import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import com.kyant.backdrop.effects.effect
import kotlinx.coroutines.launch"""
)

# Add backdrop param to MiniPlayer
content = content.replace(
    """fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
)""",
    """fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
)"""
)

# Pass backdrop to NewMiniPlayer
content = content.replace(
    """NewMiniPlayer(
        progressState = progressState,
        modifier = modifier,
    )""",
    """NewMiniPlayer(
        progressState = progressState,
        backdrop = backdrop,
        modifier = modifier,
    )"""
)

# Add backdrop param to NewMiniPlayer signature
content = content.replace(
    """private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
)""",
    """private fun NewMiniPlayer(
    progressState: ProgressState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
)"""
)

# Replace the inner Box (the pill) modifier — swap background + background-style block with shader
old_box = """.clip(RoundedCornerShape(15.dp))
                    .background(color = backgroundColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(15.dp)),
        ) {
            when (miniPlayerBackground) {
                MiniPlayerBackgroundStyle.BLUR -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        mediaMetadata?.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(60.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
                MiniPlayerBackgroundStyle.GRADIENT -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors
                    else listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainer,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors)
                            )
                            .background(Color.Black.copy(alpha = 0.15f)),
                    )
                }
                else -> {}
            }"""

new_box = """.clip(RoundedCornerShape(15.dp))
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(15.dp) },
                                effects = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val refractionHeightPx = with(density) { 20.dp.toPx() }
                                        val refractionAmountPx = with(density) { 67.dp.toPx() }
                                        val eccentricFactor = 1.0f
                                        val dispersionIntensity = 1.0f
                                        val shaderString = \"\"\"
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
                                        \"\"\".trimIndent()
                                        val shader = RuntimeShader(shaderString)
                                        val cornerRadiusPx = with(density) { 15.dp.toPx() }
                                        shader.setFloatUniform("size", size.width, size.height)
                                        shader.setFloatUniform("offset", -padding, -padding)
                                        shader.setFloatUniform("cornerRadii", floatArrayOf(cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx))
                                        shader.setFloatUniform("refractionHeight", refractionHeightPx)
                                        shader.setFloatUniform("refractionAmount", -refractionAmountPx)
                                        shader.setFloatUniform("depthEffect", eccentricFactor)
                                        shader.setFloatUniform("chromaticAberration", dispersionIntensity)
                                        effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
                                    }
                                },
                                onDrawSurface = {
                                    drawRect(backgroundColor.copy(alpha = 0.18f))
                                }
                            )
                        } else {
                            Modifier.background(color = backgroundColor)
                        }
                    )
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(15.dp)),
        ) {"""

content = content.replace(old_box, new_box)

with open(path, "w") as f:
    f.write(content)
print("✅ MiniPlayer.kt patched")
PYEOF
