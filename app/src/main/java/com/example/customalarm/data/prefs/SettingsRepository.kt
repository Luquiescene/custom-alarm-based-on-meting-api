package com.example.customalarm.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<SettingsSnapshot> = context.dataStore.data.map { p ->
        SettingsSnapshot(
            themeMode = p[KEY_THEME] ?: ThemeMode.SYSTEM.name,
            apiSelectedIndex = p[KEY_API_INDEX] ?: 1,
            apiCustomUrl = p[KEY_API_CUSTOM] ?: "",
            apiValidated = p[KEY_VALIDATED] ?: false,
            validatedBase = p[KEY_VALIDATED_BASE],
            capabilityMatrix = decodeMatrix(p[KEY_MATRIX]),
            onboardingShown = p[KEY_ONBOARDING] ?: false
        )
    }

    /** 一次性读取当前设置。 */
    suspend fun current(): SettingsSnapshot = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setApiSelection(index: Int, customUrl: String) {
        context.dataStore.edit {
            it[KEY_API_INDEX] = index
            it[KEY_API_CUSTOM] = customUrl
        }
    }

    suspend fun setValidationResult(validated: Boolean, base: String?, matrix: List<CapabilityEntry>) {
        context.dataStore.edit {
            it[KEY_VALIDATED] = validated
            if (validated && base != null) it[KEY_VALIDATED_BASE] = base else it.remove(KEY_VALIDATED_BASE)
            it[KEY_MATRIX] = json.encodeToString(matrix)
        }
    }

    /** 切换 API 或修改自定义地址后使旧校验结果作废。 */
    suspend fun invalidateValidation() {
        context.dataStore.edit {
            it[KEY_VALIDATED] = false
            it.remove(KEY_VALIDATED_BASE)
            it[KEY_MATRIX] = json.encodeToString(emptyList<CapabilityEntry>())
        }
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = shown }
    }

    private fun decodeMatrix(raw: String?): List<CapabilityEntry> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString<List<CapabilityEntry>>(raw) }.getOrDefault(emptyList())

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_API_INDEX = intPreferencesKey("api_index")
        private val KEY_API_CUSTOM = stringPreferencesKey("api_custom_url")
        private val KEY_VALIDATED = booleanPreferencesKey("api_validated")
        private val KEY_VALIDATED_BASE = stringPreferencesKey("api_validated_base")
        private val KEY_MATRIX = stringPreferencesKey("capability_matrix")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_shown")
    }
}
