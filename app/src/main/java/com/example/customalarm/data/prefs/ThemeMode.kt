package com.example.customalarm.data.prefs

/** 外观主题模式。 */
enum class ThemeMode { SYSTEM, DARK, LIGHT;

    companion object {
        fun safeValueOf(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
