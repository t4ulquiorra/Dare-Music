package com.dare.music.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.navigation.NavController
import com.dare.music.LocalPlayerConnection
import com.dare.music.R
import com.dare.music.models.MediaMetadata
import com.dare.music.ui.screens.Screens
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.effect
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAB_HOME    = 0
private const val TAB_SEARCH  = 1
private const val TAB_LIBRARY = 2

private fun liquidGlass(backdrop: Backdrop, density: Density, shaderSrc: String): Modifier =
    Modifier.drawBackdrop(
        backdrop      = backdrop,
        shape         = { RoundedCornerShape(50) },
        effects       = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val refH = with(density) { 20.dp.toPx() }
                val refA = with(density) { 67.dp.toPx() }
                val cr   = size.height / 2f
                val sh   = RuntimeShader(shaderSrc)
                sh.setFloatUniform("size",               size.width, size.height)
                sh.setFloatUniform("offset",             -padding, -padding)
                sh.setFloatUniform("cornerRadii",        floatArrayOf(cr, cr, cr, cr))
                sh.setFloatUniform("refractionHeight",   refH)
                sh.setFloatUniform("refractionAmount",   -refA)
                sh.setFloatUniform("depthEffect",        1.0f)
                sh.setFloatUniform("chromaticAberration",0.0f)
                val blur   = RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
                val shader = RenderEffect.createRuntimeShaderEffect(sh, "content")
                effect(RenderEffect.createChainEffect(shader, blur))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                effect(RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP))
            }
        },
        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.38f)) },
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiquidGlassAppBottomNavigationBar(
    navController: NavController,
    backdrop: Backdrop,
    bottomNavScreens: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onStopPlayer: () -> Unit,
    isScrolledToTop: Boolean = true,
) {
    val density = LocalDensity.current

    val playerConn  = LocalPlayerConnection.current
    val currentSong by remember(playerConn) {
        playerConn?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsState()
    val showMiniPlayer = currentSong != null

    fun routeToIndex(route: String?): Int {
        if (route == null) return TAB_HOME
        val sr = bottomNavScreens.getOrNull(TAB_SEARCH)?.route ?: "search_input"
        return when {
            route == (bottomNavScreens.getOrNull(TAB_HOME)?.route    ?: "home")    -> TAB_HOME
            route == sr || route.startsWith("search/")                              -> TAB_SEARCH
            route == (bottomNavScreens.getOrNull(TAB_LIBRARY)?.route ?: "library") -> TAB_LIBRARY
            else -> TAB_HOME
        }
    }

    var selectedIndex         by rememberSaveable { mutableIntStateOf(routeToIndex(currentRoute)) }
    var previousSelectedIndex by rememberSaveable { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(currentRoute) {
        val idx = routeToIndex(currentRoute)
        if (idx != selectedIndex) { previousSelectedIndex = selectedIndex; selectedIndex = idx }
    }

    val searchRoute           = bottomNavScreens.getOrNull(TAB_SEARCH)?.route ?: "search_input"
    val isInSearchDestination = remember(currentRoute) {
        currentRoute == searchRoute || currentRoute?.startsWith("search/") == true
    }

    var isExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(isInSearchDestination) { isExpanded = !isInSearchDestination }
    LaunchedEffect(isScrolledToTop) { if (!isInSearchDestination) isExpanded = isScrolledToTop }

    var updateConstraints by remember { mutableStateOf(true) }
    var constraintSet     by remember { mutableStateOf(buildConstraintSet(showMiniPlayer, isExpanded)) }
    LaunchedEffect(showMiniPlayer, isExpanded) {
        constraintSet = buildConstraintSet(showMiniPlayer, isExpanded); updateConstraints = false
    }
    LaunchedEffect(updateConstraints) {
        if (updateConstraints) { constraintSet = buildConstraintSet(showMiniPlayer, isExpanded); updateConstraints = false }
    }

    val glass = liquidGlass(backdrop, density, uniform shader content;\nuniform float2 size;\nuniform float2 offset;\nuniform float4 cornerRadii;\nuniform float refractionHeight;\nuniform float refractionAmount;\nuniform float depthEffect;\nuniform float chromaticAberration;\n\nfloat radiusAt(float2 coord, float4 radii) {\n    if (coord.x >= 0.0) {\n        if (coord.y <= 0.0) return radii.y;\n        else return radii.z;\n    } else {\n        if (coord.y <= 0.0) return radii.x;\n        else return radii.w;\n    }\n}\n\nfloat signedDistRoundedRect(float2 coord, float2 halfSize, float radius) {\n    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n    float outsideDist = length(max(cornerCoord, 0.0)) - radius;\n    float insideDist  = min(max(cornerCoord.x, cornerCoord.y), 0.0);\n    return outsideDist + insideDist;\n}\n\nfloat2 gradSignedDistRoundedRect(float2 coord, float2 halfSize, float radius) {\n    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {\n        return sign(coord) * normalize(max(cornerCoord, 0.0));\n    } else {\n        float gradX = step(cornerCoord.y, cornerCoord.x);\n        return sign(coord) * float2(gradX, 1.0 - gradX);\n    }\n}\n\nfloat circleMap(float xVal) {\n    return 1.0 - sqrt(1.0 - xVal * xVal);\n}\n\nhalf4 main(float2 coord) {\n    float2 halfSize      = size * 0.5;\n    float2 centeredCoord = (coord + offset) - halfSize;\n    float  radius        = radiusAt(coord, cornerRadii);\n    float  dist          = signedDistRoundedRect(centeredCoord, halfSize, radius);\n    if (-dist >= refractionHeight) { return content.eval(coord); }\n    dist = min(dist, 0.0);\n    float  dispAmount   = circleMap(1.0 - (-dist / refractionHeight)) * refractionAmount;\n    float  gradRadius   = min(radius * 1.5, min(halfSize.x, halfSize.y));\n    float2 gradVec      = normalize(gradSignedDistRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));\n    float2 refractCoord = coord + dispAmount * gradVec;\n    float  dispersion   = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));\n    float2 disperseVec  = dispAmount * gradVec * dispersion;\n    half4 result = half4(0.0);\n    half4 s0 = content.eval(refractCoord + disperseVec); result.r += s0.r/3.5; result.a += s0.a/7.0;\n    half4 s1 = content.eval(refractCoord + disperseVec*(2.0/3.0)); result.r += s1.r/3.5; result.g += s1.g/7.0; result.a += s1.a/7.0;\n    half4 s2 = content.eval(refractCoord + disperseVec*(1.0/3.0)); result.r += s2.r/3.5; result.g += s2.g/3.5; result.a += s2.a/7.0;\n    half4 s3 = content.eval(refractCoord); result.g += s3.g/3.5; result.a += s3.a/7.0;\n    half4 s4 = content.eval(refractCoord - disperseVec*(1.0/3.0)); result.g += s4.g/3.5; result.b += s4.b/3.0; result.a += s4.a/7.0;\n    half4 s5 = content.eval(refractCoord - disperseVec*(2.0/3.0)); result.b += s5.b/3.0; result.a += s5.a/7.0;\n    half4 s6 = content.eval(refractCoord - disperseVec); result.r += s6.r/7.0; result.b += s6.b/3.0; result.a += s6.a/7.0;\n    return result;\n}\n)

    ConstraintLayout(
        constraintSet      = constraintSet,
        modifier           = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(bottom = 8.dp)
            .imePadding(),
        animateChangesSpec = tween<Float>(300),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier
                .padding(start = 16.dp)
                .wrapContentSize()
                .layoutId("toolbar"),
        ) {
            Box(
                modifier = Modifier
                    .then(if (!isExpanded) Modifier.size(48.dp) else Modifier.wrapContentSize())
                    .clip(RoundedCornerShape(50))
                    .then(glass)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .onGloballyPositioned { updateConstraints = true },
            ) {
                HorizontalFloatingToolbar(
                    modifier       = Modifier.wrapContentSize(),
                    contentPadding = PaddingValues(horizontal = if (isExpanded) 4.dp else 0.dp),
                    colors         = FloatingToolbarDefaults
                        .standardFloatingToolbarColors()
                        .copy(toolbarContainerColor = Color.Transparent),
                    expanded       = isExpanded,
                    trailingContent = {
                        bottomNavScreens
                            .filterIndexed { index, _ -> index != TAB_SEARCH }
                            .forEach { screen ->
                                val idx = bottomNavScreens.indexOf(screen)
                                Button(
                                    onClick = {
                                        val sel = selectedIndex == idx
                                        if (!sel) { previousSelectedIndex = selectedIndex; selectedIndex = idx }
                                        onItemClick(screen, sel)
                                    },
                                    shape  = CircleShape,
                                    colors = ButtonDefaults.buttonColors().copy(
                                        containerColor         = if (selectedIndex == idx) Color.White.copy(alpha = 0.20f) else Color.Transparent,
                                        contentColor           = Color.White,
                                        disabledContainerColor = Color.Transparent,
                                    ),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector        = if (idx == TAB_HOME) Icons.Rounded.Home else Icons.Rounded.LibraryMusic,
                                            contentDescription = null,
                                            tint               = Color.White,
                                        )
                                        Text(
                                            text  = stringResource(if (idx == TAB_HOME) R.string.home else R.string.filter_library),
                                            style = if (selectedIndex == idx) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                    else MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                    },
                ) {
                    if (!isExpanded) {
                        IconButton(
                            modifier = Modifier.size(FloatingToolbarDefaults.ContainerSize.value.dp),
                            shape    = CircleShape,
                            onClick  = {
                                if (selectedIndex == TAB_SEARCH) {
                                    val dest = bottomNavScreens.getOrNull(previousSelectedIndex) ?: bottomNavScreens.first()
                                    selectedIndex = previousSelectedIndex; previousSelectedIndex = TAB_SEARCH
                                    onItemClick(dest, false)
                                } else { isExpanded = true }
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                        ) {
                            Icon(
                                imageVector = when (selectedIndex) {
                                    TAB_HOME    -> Icons.Rounded.Home
                                    TAB_SEARCH  -> if (previousSelectedIndex == TAB_LIBRARY) Icons.Rounded.LibraryMusic else Icons.Rounded.Home
                                    TAB_LIBRARY -> Icons.Rounded.LibraryMusic
                                    else        -> Icons.Outlined.Home
                                },
                                contentDescription = null,
                            )
                        }
                    }
                }
            }

            if (isExpanded) Spacer(Modifier.size(12.dp))

            AnimatedVisibility(
                visible = !isInSearchDestination && isExpanded,
                enter   = slideInHorizontally(tween(100)) { it / 2 },
                exit    = slideOutHorizontally(tween(100)) { -it / 2 },
            ) {
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .then(glass)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingActionButton(
                        modifier       = Modifier.size(56.dp),
                        elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        onClick        = {
                            previousSelectedIndex = selectedIndex; selectedIndex = TAB_SEARCH
                            bottomNavScreens.getOrNull(TAB_SEARCH)?.let { onItemClick(it, false) }
                        },
                        shape          = CircleShape,
                        containerColor = Color.Transparent,
                        contentColor   = Color.Transparent,
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        DareMiniPlayer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(56.dp)
                .layoutId("miniPlayer"),
            onClick  = onOpenNowPlaying,
            onClose  = onStopPlayer,
        )
    }
}

private fun buildConstraintSet(showMiniPlayer: Boolean, isExpanded: Boolean): ConstraintSet =
    ConstraintSet {
        val toolbar    = createRefFor("toolbar")
        val miniPlayer = createRefFor("miniPlayer")
        constrain(toolbar) {
            bottom.linkTo(parent.bottom); start.linkTo(parent.start)
            if (isExpanded) end.linkTo(parent.end)
            width = Dimension.wrapContent; height = Dimension.wrapContent
        }
        constrain(miniPlayer) {
            if (!isExpanded) {
                start.linkTo(toolbar.end); end.linkTo(parent.end)
                top.linkTo(toolbar.top); bottom.linkTo(toolbar.bottom)
                width = if (showMiniPlayer) Dimension.fillToConstraints else Dimension.wrapContent
            } else {
                start.linkTo(parent.start); end.linkTo(parent.end)
                bottom.linkTo(toolbar.top, margin = 12.dp)
                width = if (showMiniPlayer) Dimension.matchParent else Dimension.wrapContent
            }
            visibility = if (showMiniPlayer) Visibility.Visible else Visibility.Gone
        }
    }
