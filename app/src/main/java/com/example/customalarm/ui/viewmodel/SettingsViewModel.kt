package com.example.customalarm.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customalarm.data.prefs.CapabilityEntry
import com.example.customalarm.data.prefs.SettingsSnapshot
import com.example.customalarm.data.prefs.ThemeMode
import com.example.customalarm.data.prefs.resolveBaseUrl
import com.example.customalarm.di.AppContainer
import com.example.customalarm.util.CustomAlarmLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsSnapshot())

    var testing by mutableStateOf(false)
    var testMessage by mutableStateOf<String?>(null)
    var matrix by mutableStateOf<List<CapabilityEntry>>(emptyList())

    init {
        viewModelScope.launch { matrix = container.settingsRepository.current().capabilityMatrix }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setApiSelection(index: Int, customUrl: String) {
        viewModelScope.launch {
            container.settingsRepository.setApiSelection(index, customUrl)
            container.settingsRepository.invalidateValidation()
            testMessage = null
            matrix = emptyList()
        }
    }

    fun testConnectivity() {
        testing = true
        testMessage = null
        viewModelScope.launch {
            try {
                val base = resolveBaseUrl(container.settingsRepository.current())
                if (base.isBlank()) {
                    testMessage = "请先填写有效的 API 地址"
                    testing = false
                    return@launch
                }
                val results = container.apiValidation.probe(base)
                matrix = results
                val netease = results.firstOrNull { it.server == "netease" }
                val passed = netease?.passed == true
                container.settingsRepository.setValidationResult(passed, if (passed) base else null, results)
                testMessage = if (passed) "校验通过" else "校验未通过"
                container.alarmScheduler.rescheduleAll()
                CustomAlarmLog.i("API 校验完成：passed=$passed")
            } catch (e: Exception) {
                testMessage = "校验异常：${e.message}"
                CustomAlarmLog.e("API 校验异常", e)
            } finally {
                testing = false
            }
        }
    }

    fun setOnboardingShown(shown: Boolean) {
        viewModelScope.launch { container.settingsRepository.setOnboardingShown(shown) }
    }
}
