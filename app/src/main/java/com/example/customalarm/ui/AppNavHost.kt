package com.example.customalarm.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.customalarm.ui.screens.AboutScreen
import com.example.customalarm.ui.screens.AlarmEditScreen
import com.example.customalarm.ui.screens.AlarmListScreen
import com.example.customalarm.ui.screens.DebugScreen
import com.example.customalarm.ui.screens.OnboardingScreen
import com.example.customalarm.ui.screens.SettingsScreen
import com.example.customalarm.ui.screens.UsageScreen

@Composable
fun AppNavHost(startDestination: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable("list") {
            AlarmListScreen(
                onAdd = { nav.navigate("edit/${com.example.customalarm.ui.viewmodel.AlarmEditViewModel.NEW_ALARM_ID}") },
                onEdit = { id -> nav.navigate("edit/$id") },
                onSettings = { nav.navigate("settings") },
                onDebug = { nav.navigate("debug") }
            )
        }
        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            AlarmEditScreen(
                alarmId = entry.arguments?.getLong("id") ?: -1L,
                onBack = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onUsage = { nav.navigate("usage") },
                onAbout = { nav.navigate("about") }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onDone = { nav.popBackStack() },
                onUsage = { nav.navigate("usage") }
            )
        }
        composable("usage") {
            UsageScreen(onBack = { nav.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { nav.popBackStack() })
        }
        composable("debug") {
            DebugScreen(onBack = { nav.popBackStack() })
        }
    }
}
