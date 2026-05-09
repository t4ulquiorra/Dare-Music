/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.dare.music.constants.DarkModeKey
import com.dare.music.constants.PureBlackKey
import com.dare.music.ui.navigation.graph.homeScreenGraph
import com.dare.music.ui.navigation.graph.libraryScreenGraph
import com.dare.music.ui.navigation.graph.listScreenGraph
import com.dare.music.ui.navigation.graph.loginScreenGraph
import com.dare.music.ui.navigation.graph.settingsScreenGraph
import com.dare.music.ui.screens.equalizer.EqScreen
import com.dare.music.ui.screens.library.LibraryScreen
import com.dare.music.ui.screens.search.OnlineSearchResult
import com.dare.music.ui.screens.search.SearchScreen
import com.dare.music.ui.screens.settings.DarkMode
import com.dare.music.ui.screens.wrapped.WrappedScreen
import com.dare.music.utils.rememberEnumPreference
import com.dare.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    // Bottom bar destinations
    composable(Screens.Home.route) {
        HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = true)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle,
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    composable("listen_together_from_topbar") {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = { fadeOut(tween(200)) },
    ) { backStackEntry ->
        OnlineSearchResult(
            navController = navController,
            savedStateHandle = backStackEntry.savedStateHandle,
        )
    }

    composable("wrapped") {
        WrappedScreen(navController)
    }

    dialog("equalizer") {
        EqScreen()
    }

    // Home screen graph
    homeScreenGraph(
        navController = navController,
    )

    // Library screen graph
    libraryScreenGraph(
        navController = navController,
    )

    // List screen graph
    listScreenGraph(
        navController = navController,
        scrollBehavior = scrollBehavior,
    )

    // Login screen graph
    loginScreenGraph(
        navController = navController,
        latestVersionName = latestVersionName,
    )

    // Settings screen graph
    settingsScreenGraph(
        navController = navController,
        scrollBehavior = scrollBehavior,
        latestVersionName = latestVersionName,
        activity = activity,
        snackbarHostState = snackbarHostState,
    )
}
