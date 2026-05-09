/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.navigation.graph

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dare.music.ui.screens.AlbumScreen
import com.dare.music.ui.screens.BrowseScreen
import com.dare.music.ui.screens.YouTubeBrowseScreen
import com.dare.music.ui.screens.artist.ArtistAlbumsScreen
import com.dare.music.ui.screens.artist.ArtistItemsScreen
import com.dare.music.ui.screens.artist.ArtistScreen
import com.dare.music.ui.screens.artist.ArtistSongsScreen
import com.dare.music.ui.screens.playlist.OnlinePlaylistScreen
import com.dare.music.ui.screens.podcast.OnlinePodcastScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.listScreenGraph(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController)
    }

    composable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("isPodcastChannel") {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        ArtistScreen(navController)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController)
    }

    composable(
        route = "online_podcast/{podcastId}",
        arguments = listOf(
            navArgument("podcastId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            },
        ),
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }
}
