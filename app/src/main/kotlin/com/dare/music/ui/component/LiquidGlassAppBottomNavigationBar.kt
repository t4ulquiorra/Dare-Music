package com.dare.music.ui.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.Color
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

// Shared blur effect used on both the toolbar pill and the search FAB
private fun Modifier.glassBlur(backdrop: Backdrop): Modifier =
    drawBackdrop(
        backdrop      = backdrop,
        shape         = { RoundedCornerShape(50) },
        effects       = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                effect(
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                )
            }
        },
        onDrawSurface = {
            // Dark tinted frosted-glass surface — same approach as MiniPlayer.kt
            drawRect(Color.Black.copy(alpha = 0.38f))
        },
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

    // ── Mini-player visibility ───────────────────────────────────────────────
    val playerConn  = LocalPlayerConnection.current
    val currentSong by remember(playerConn) {
        playerConn?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsState()
    val showMiniPlayer = currentSong != null

    // ── Nav state ────────────────────────────────────────────────────────────
    fun routeToIndex(route: String?): Int {
        if (route == null) return TAB_HOME
        val searchRoute = bottomNavScreens.getOrNull(TAB_SEARCH)?.route ?: "search_input"
        return when {
            route == (bottomNavScreens.getOrNull(TAB_HOME)?.route ?: "home")       -> TAB_HOME
            route == searchRoute || route.startsWith("search/")                     -> TAB_SEARCH
            route == (bottomNavScreens.getOrNull(TAB_LIBRARY)?.route ?: "library") -> TAB_LIBRARY
            else -> TAB_HOME
        }
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

    val searchRoute = bottomNavScreens.getOrNull(TAB_SEARCH)?.route ?: "search_input"
    val isInSearchDestination = remember(currentRoute) {
        currentRoute == searchRoute || currentRoute?.startsWith("search/") == true
    }

    var isExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(isInSearchDestination) { isExpanded = !isInSearchDestination }
    LaunchedEffect(isScrolledToTop) { if (!isInSearchDestination) isExpanded = isScrolledToTop }

    // Explicit types avoid T-inference errors with ConstraintSet
    var updateConstraints: Boolean by remember { mutableStateOf(true) }
    var constraintSet: ConstraintSet by remember {
        mutableStateOf(buildConstraintSet(showMiniPlayer = showMiniPlayer, isExpanded = isExpanded))
    }
    LaunchedEffect(showMiniPlayer, isExpanded) {
        constraintSet = buildConstraintSet(showMiniPlayer = showMiniPlayer, isExpanded = isExpanded)
        updateConstraints = false
    }
    LaunchedEffect(updateConstraints) {
        if (updateConstraints) {
            constraintSet = buildConstraintSet(showMiniPlayer = showMiniPlayer, isExpanded = isExpanded)
            updateConstraints = false
        }
    }

    ConstraintLayout(
        constraintSet      = constraintSet,
        modifier           = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(bottom = 8.dp)
            .imePadding(),
        animateChangesSpec = tween<Float>(300),
    ) {
        // ── Toolbar pill ──────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .wrapContentSize()
                .layoutId("toolbar"),
        ) {
            HorizontalFloatingToolbar(
                modifier = Modifier
                    .then(if (!isExpanded) Modifier.size(48.dp) else Modifier.wrapContentSize())
                    // Glass blur applied AFTER size is set so the shape covers the right area
                    .glassBlur(backdrop)
                    .onGloballyPositioned { updateConstraints = true },
                contentPadding = PaddingValues(horizontal = if (isExpanded) 4.dp else 0.dp),
                colors = FloatingToolbarDefaults
                    .standardFloatingToolbarColors()
                    .copy(toolbarContainerColor = Color.Transparent),
                expanded = isExpanded,
                trailingContent = {
                    var buttonSize by remember { mutableStateOf(0.dp to 0.dp) }
                    // Home + Library (Search is separate FAB)
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
                                        containerColor       = if (selectedIndex == idx) Color.White.copy(alpha = 0.20f) else Color.Transparent,
                                        contentColor         = Color.White,
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
                                            style = if (selectedIndex == idx) {
                                                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            } else {
                                                MaterialTheme.typography.bodySmall
                                            },
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                },
            ) {
                // Collapsed icon — shows selected tab
                if (!isExpanded) {
                    IconButton(
                        modifier = Modifier.size(FloatingToolbarDefaults.ContainerSize.value.dp),
                        shape    = CircleShape,
                        onClick  = {
                            if (selectedIndex == TAB_SEARCH) {
                                val destination = bottomNavScreens.getOrNull(previousSelectedIndex)
                                    ?: bottomNavScreens.first()
                                selectedIndex         = previousSelectedIndex
                                previousSelectedIndex = TAB_SEARCH
                                onItemClick(destination, false)
                            } else {
                                isExpanded = true
                            }
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

            if (isExpanded) Spacer(Modifier.size(12.dp))

            // ── Search FAB ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isInSearchDestination && isExpanded,
                enter   = slideInHorizontally(tween(100)) { it / 2 },
                exit    = slideOutHorizontally(tween(100)) { -it / 2 },
            ) {
                FloatingActionButton(
                    modifier       = Modifier.glassBlur(backdrop),
                    elevation      = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    onClick        = {
                        previousSelectedIndex = selectedIndex
                        selectedIndex         = TAB_SEARCH
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

        // ── Mini-player ───────────────────────────────────────────────────────
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
