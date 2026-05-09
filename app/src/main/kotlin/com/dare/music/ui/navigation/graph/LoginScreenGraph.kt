/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.dare.music.ui.screens.LoginScreen
import com.dare.music.ui.screens.settings.AccountSettings
import com.dare.music.ui.screens.settings.DiscordLoginScreen

fun NavGraphBuilder.loginScreenGraph(
    navController: NavHostController,
    latestVersionName: String,
) {
    composable("login") {
        LoginScreen(navController)
    }

    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }

    composable("account_settings") {
        AccountSettings(
            navController = navController,
            onClose = { navController.navigateUp() },
            latestVersionName = latestVersionName,
        )
    }
}
