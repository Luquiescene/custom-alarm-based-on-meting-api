package com.example.customalarm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.customalarm.data.prefs.ThemeMode
import com.example.customalarm.di.AppViewModelProvider
import com.example.customalarm.ui.theme.CustomAlarmTheme
import com.example.customalarm.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settingsVm: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            CustomAlarmTheme(ThemeMode.safeValueOf(settings.themeMode)) {
                AppNavHost(startDestination = if (settings.onboardingShown) "list" else "onboarding")
            }
        }
    }
}
