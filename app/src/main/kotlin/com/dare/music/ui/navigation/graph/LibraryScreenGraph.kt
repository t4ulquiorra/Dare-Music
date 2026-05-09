/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.navigation.graph

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dare.music.ui.screens.playlist.AutoPlaylistScreen
import com.dare.music.ui.screens.playlist.CachePlaylistScreen
import com.dare.music.ui.screens.playlist.LocalPlaylistScreen
import com.dare.music.ui.screens.playlist.TopPlaylistScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.libraryScreenGraph(
    navController: NavHostController,
) {
    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController)
    }

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController)
    }
}
