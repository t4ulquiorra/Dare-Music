/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.navigation.graph

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.dare.music.ui.screens.HistoryScreen
import com.dare.music.ui.screens.MoodAndGenresScreen
import com.dare.music.ui.screens.NewReleaseScreen
import com.dare.music.ui.screens.StatsScreen
import com.dare.music.ui.screens.AccountScreen
import com.dare.music.ui.screens.ChartsScreen
import com.dare.music.ui.screens.recognition.RecognitionHistoryScreen
import com.dare.music.ui.screens.recognition.RecognitionScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.homeScreenGraph(
    navController: NavHostController,
) {
    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController)
    }

    composable("account") {
        AccountScreen(navController)
    }

    composable("new_release") {
        NewReleaseScreen(navController)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments = listOf(
            navArgument("autoStart") {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }
}
