package com.example.customalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.customalarm.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF8F4C00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2F1500),
    secondary = Color(0xFF745943),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A14),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A14),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4E2600),
    primaryContainer = Color(0xFF6B3700),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE3C0A5),
    onSecondary = Color(0xFF422B19),
    background = Color(0xFF201A17),
    onBackground = Color(0xFFECE0D9),
    surface = Color(0xFF201A17),
    onSurface = Color(0xFFECE0D9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/** 三选一主题：跟随系统 / 深色 / 浅色。 */
@Composable
fun CustomAlarmTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
