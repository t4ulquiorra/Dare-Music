/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.navigation

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.dare.music.ui.screens.Screens
import com.dare.music.ui.screens.navigationBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationGraph(
    navController: NavHostController,
    startDestination: String,
    navigationItems: List<Screens>,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            val previousRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                slideInHorizontally { it / 8 } + fadeIn(tween(200))
            } else {
                slideInHorizontally { -it / 8 } + fadeIn(tween(200))
            }
        },
        exitTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            val targetRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
            } else {
                slideOutHorizontally { it / 8 } + fadeOut(tween(200))
            }
        },
        popEnterTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            val previousRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                slideInHorizontally { it / 8 } + fadeIn(tween(200))
            } else {
                slideInHorizontally { -it / 8 } + fadeIn(tween(200))
            }
        },
        popExitTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            val targetRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
            } else {
                slideOutHorizontally { it / 8 } + fadeOut(tween(200))
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        navigationBuilder(
            navController = navController,
            scrollBehavior = scrollBehavior,
            latestVersionName = latestVersionName,
            activity = activity,
            snackbarHostState = snackbarHostState,
        )
    }
}
