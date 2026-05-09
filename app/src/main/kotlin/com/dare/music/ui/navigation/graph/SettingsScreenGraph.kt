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
import com.dare.music.ui.screens.settings.AboutScreen
import com.dare.music.ui.screens.settings.AiSettings
import com.dare.music.ui.screens.settings.AndroidAutoSettings
import com.dare.music.ui.screens.settings.AppearanceSettings
import com.dare.music.ui.screens.settings.BackupAndRestore
import com.dare.music.ui.screens.settings.ContentSettings
import com.dare.music.ui.screens.settings.PlayerSettings
import com.dare.music.ui.screens.settings.PrivacySettings
import com.dare.music.ui.screens.settings.RomanizationSettings
import com.dare.music.ui.screens.settings.SettingsScreen
import com.dare.music.ui.screens.settings.StorageSettings
import com.dare.music.ui.screens.settings.ThemeScreen
import com.dare.music.ui.screens.settings.UpdaterScreen
import com.dare.music.ui.screens.settings.integrations.DiscordSettings
import com.dare.music.ui.screens.settings.integrations.IntegrationScreen
import com.dare.music.ui.screens.settings.integrations.LastFMSettings
import com.dare.music.ui.screens.settings.integrations.ListenTogetherSettings

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.settingsScreenGraph(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    composable("settings") {
        SettingsScreen(navController, latestVersionName)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, activity, snackbarHostState)
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/content") {
        ContentSettings(navController)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    composable("settings/ai") {
        AiSettings(navController)
    }

    composable("settings/player") {
        PlayerSettings(navController)
    }

    composable("settings/storage") {
        StorageSettings(navController)
    }

    composable("settings/privacy") {
        PrivacySettings(navController)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable("settings/integrations") {
        IntegrationScreen(navController)
    }

    composable("settings/integrations/discord") {
        DiscordSettings(navController, snackbarHostState)
    }

    composable("settings/integrations/lastfm") {
        LastFMSettings(navController)
    }

    composable("settings/integrations/listen_together") {
        ListenTogetherSettings(navController)
    }

    composable("settings/updater") {
        UpdaterScreen(navController)
    }

    composable("settings/about") {
        AboutScreen(navController)
    }

    composable("settings/android_auto") {
        AndroidAutoSettings(navController, scrollBehavior)
    }
}
