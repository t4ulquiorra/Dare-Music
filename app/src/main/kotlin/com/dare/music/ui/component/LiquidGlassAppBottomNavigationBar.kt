package com.dare.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.core.graphics.scale
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dare.music.R
import com.dare.music.ui.screens.Screens
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.graphics.lerp as colorLerp

// Tab ordinals
private const val TAB_HOME    = 0
private const val TAB_SEARCH  = 1
private const val TAB_LIBRARY = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiquidGlassAppBottomNavigationBar(
    navController: NavController,
    backdrop: Backdrop,
    // Pass Screens.MainScreens.take(3) — must be [Home, Search, Library] in that order
    bottomNavScreens: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onStopPlayer: () -> Unit,
    isScrolledToTop: Boolean = true,
) {
    val density       = LocalDensity.current
    val layer         = rememberGraphicsLayer()
    val luminanceAnim = remember { Animatable(0f) }

    // Luminance-aware pill colour — same logic as Xevrae
    val pillColor by animateColorAsState(
        targetValue = colorLerp(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            luminanceAnim.value * 1.25f,
        ),
        animationSpec = tween(1000),
        label = "PillColor",
    )

    // Sample background luminance every second to drive colour animation
    LaunchedEffect(layer) {
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val bmp = layer.toImageBitmap()
                        .asAndroidBitmap()
                        .scale(5, 5, false)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    buffer.rewind()
                    bmp.copyPixelsToBuffer(buffer)
                }
            } catch (_: Exception) {}
            val avg = (0 until 25).sumOf { i ->
                val c = buffer.get(i)
                val r = (c shr 16 and 0xFF) / 255.0
                val g = (c shr  8 and 0xFF) / 255.0
                val b = (c        and 0xFF) / 255.0
                0.2126 * r + 0.7152 * g + 0.0722 * b
            } / 25.0
            luminanceAnim.animateTo(avg.coerceAtMost(0.8).toFloat(), tween(500))
            delay(1.seconds)
        }
    }

    // ── Nav state ────────────────────────────────────────────────────────────
    fun routeToIndex(route: String?): Int = when {
        route == null -> TAB_HOME
        route == bottomNavScreens.getOrNull(TAB_HOME)?.route    -> TAB_HOME
        route.startsWith(bottomNavScreens.getOrNull(TAB_SEARCH)?.route.orEmpty()) -> TAB_SEARCH
        route == bottomNavScreens.getOrNull(TAB_LIBRARY)?.route -> TAB_LIBRARY
        else -> TAB_HOME
    }

    var selectedIndex         by rememberSaveable { mutableIntStateOf(routeToIndex(currentRoute)) }
    var previousSelectedIndex by rememberSaveable { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(currentRoute) {
        val idx = routeToIndex(currentRoute)
        if (idx != selectedIndex) {
            previousSelectedIndex = selectedIndex
            selectedIndex = idx
        }
    }

    val isInSearchDestination = remember(currentRoute) {
        currentRoute?.startsWith(
            bottomNavScreens.getOrNull(TAB_SEARCH)?.route.orEmpty()
        ) == true
    }

    var isExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(isInSearchDestination) { isExpanded = !isInSearchDestination }
    LaunchedEffect(isScrolledToTop)       { if (!isInSearchDestination) isExpanded = isScrolledToTop }

    // ── ConstraintSet ────────────────────────────────────────────────────────
    // Mini-player visibility: show whenever LocalPlayerConnection has a current song.
    // DareMiniPlayer returns early when currentSong == null so Gone/Visible is cosmetic.
    var updateConstraints by remember { mutableStateOf(true) }
    var constraintSet     by remember { mutableStateOf(buildConstraintSet(showMiniPlayer = true, isExpanded = isExpanded)) }

    LaunchedEffect(isExpanded) {
        constraintSet = buildConstraintSet(showMiniPlayer = true, isExpanded = isExpanded)
        updateConstraints = false
    }
    LaunchedEffect(updateConstraints) {
        if (updateConstraints) {
            constraintSet = buildConstraintSet(showMiniPlayer = true, isExpanded = isExpanded)
            updateConstraints = false
        }
    }

    val searchIconTint by animateColorAsState(
        targetValue = if (luminanceAnim.value > 0.6f) Color.Black else Color.White,
        animationSpec = tween(500),
        label = "SearchIconTint",
    )

    ConstraintLayout(
        constraintSet = constraintSet,
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(bottom = 8.dp)
            .imePadding(),
        animateChangesSpec = tween(300),
    ) {
        // ── Toolbar pill ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .wrapContentSize()
                .layoutId("toolbar"),
        ) {
            HorizontalFloatingToolbar(
                modifier = Modifier
                    // TODO: uncomment once drawBackdropCustomShape is extracted
                    // .drawBackdropCustomShape(backdrop, layer, luminanceAnim.value, CircleShape)
                    .then(
                        if (!isExpanded) Modifier.size(48.dp) else Modifier.wrapContentSize()
                    )
                    .onGloballyPositioned { updateConstraints = true },
                contentPadding = PaddingValues(horizontal = if (isExpanded) 4.dp else 0.dp),
                colors = FloatingToolbarDefaults
                    .standardFloatingToolbarColors()
                    .copy(toolbarContainerColor = Color.Transparent),
                expanded = isExpanded,
                trailingContent = {
                    var buttonSize by remember { mutableStateOf(0.dp to 0.dp) }

                    // Home + Library only — Search is the separate FAB
                    bottomNavScreens
                        .filterIndexed { index, _ -> index != TAB_SEARCH }
                        .forEach { screen ->
                            val idx = bottomNavScreens.indexOf(screen)
                            Box {
                                Button(
                                    modifier = Modifier.onGloballyPositioned {
                                        if (selectedIndex == idx) {
                                            buttonSize = with(density) {
                                                it.size.width.toDp() to it.size.height.toDp()
                                            }
                                        }
                                    },
                                    onClick = {
                                        val isSelected = selectedIndex == idx
                                        if (!isSelected) {
                                            previousSelectedIndex = selectedIndex
                                            selectedIndex = idx
                                        }
                                        onItemClick(screen, isSelected)
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors().copy(
                                        containerColor = if (selectedIndex == idx) pillColor else Color.Transparent,
                                        contentColor   = if (selectedIndex == idx) MaterialTheme.colorScheme.primary else Color.White,
                                        disabledContainerColor = Color.Transparent,
                                    ),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (idx == TAB_HOME) Icons.Rounded.Home else Icons.Rounded.LibraryMusic,
                                            contentDescription = null,
                                        )
                                        Text(
                                            text = stringResource(
                                                if (idx == TAB_HOME) R.string.home else R.string.filter_library
                                            ),
                                            style = if (selectedIndex == idx) {
                                                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            } else {
                                                MaterialTheme.typography.bodySmall
                                            },
                                            color = if (selectedIndex == idx) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White
                                            },
                                        )
                                    }
                                }
                            }
                        }
                },
            ) {
                // Collapsed icon — selected tab icon, or home/library when in Search
                if (!isExpanded) {
                    IconButton(
                        modifier = Modifier.size(FloatingToolbarDefaults.ContainerSize.value.dp),
                        shape = CircleShape,
                        onClick = {
                            if (selectedIndex == TAB_SEARCH) {
                                val destination = bottomNavScreens.getOrNull(previousSelectedIndex)
                                    ?: bottomNavScreens.first()
                                selectedIndex = previousSelectedIndex
                                previousSelectedIndex = TAB_SEARCH
                                onItemClick(destination, false)
                            } else {
                                isExpanded = true
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = when (selectedIndex) {
                                TAB_HOME    -> Icons.Rounded.Home
                                TAB_SEARCH  -> when (previousSelectedIndex) {
                                    TAB_LIBRARY -> Icons.Rounded.LibraryMusic
                                    else        -> Icons.Rounded.Home
                                }
                                TAB_LIBRARY -> Icons.Rounded.LibraryMusic
                                else        -> Icons.Outlined.Home
                            },
                            contentDescription = null,
                        )
                    }
                }
            }

            if (isExpanded) Spacer(Modifier.size(12.dp))

            // Search FAB
            AnimatedVisibility(
                visible = !isInSearchDestination && isExpanded,
                enter = slideInHorizontally(tween(100)) { it / 2 },
                exit  = slideOutHorizontally(tween(100)) { -it / 2 },
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        // TODO: uncomment once drawBackdropCustomShape is extracted
                        // .drawBackdropCustomShape(backdrop, layer, luminanceAnim.value, CircleShape)
                        ,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    onClick = {
                        previousSelectedIndex = selectedIndex
                        selectedIndex = TAB_SEARCH
                        bottomNavScreens.getOrNull(TAB_SEARCH)?.let { onItemClick(it, false) }
                    },
                    shape = CircleShape,
                    containerColor = Color.Transparent,
                    contentColor   = Color.Transparent,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = searchIconTint)
                }
            }
        }

        // ── Mini-player ──────────────────────────────────────────────────────
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

// Direct port of Xevrae's decoupledConstraints
private fun buildConstraintSet(
    showMiniPlayer: Boolean,
    isExpanded: Boolean,
): ConstraintSet = ConstraintSet {
    val toolbar    = createRefFor("toolbar")
    val miniPlayer = createRefFor("miniPlayer")

    constrain(toolbar) {
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
        if (isExpanded) end.linkTo(parent.end)
        width  = Dimension.wrapContent
        height = Dimension.wrapContent
    }

    constrain(miniPlayer) {
        if (!isExpanded) {
            start.linkTo(toolbar.end)
            end.linkTo(parent.end)
            top.linkTo(toolbar.top)
            bottom.linkTo(toolbar.bottom)
            width = if (showMiniPlayer) Dimension.fillToConstraints else Dimension.wrapContent
        } else {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(toolbar.top, margin = 12.dp)
            width = if (showMiniPlayer) Dimension.matchParent else Dimension.wrapContent
        }
        visibility = if (showMiniPlayer) Visibility.Visible else Visibility.Gone
    }
}
