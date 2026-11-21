package com.example.customalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.prefs.SettingsSnapshot
import com.example.customalarm.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsSnapshot())

    val alarms = container.alarmRepository.observeAlarms()
        .map { list -> list.sortedWith(compareBy<AlarmEntity> { it.hour }.thenBy { it.minute }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var enableError by mutableStateOf<String?>(null)
        private set

    fun clearEnableError() { enableError = null }

    init {
        viewModelScope.launch { container.alarmScheduler.rescheduleAll() }
    }

    fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val s = container.settingsRepository.current()
            if (enabled && !s.apiValidated) {
                enableError = "请先在「设置」中完成 API 校验后再启用闹钟"
                return@launch
            }
            val updated = alarm.copy(enabled = enabled, snoozeCount = 0)
            container.alarmRepository.update(updated)
            container.alarmScheduler.rescheduleOne(updated)
            enableError = null
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            container.alarmScheduler.cancel(alarm.id)
            container.alarmRepository.delete(alarm)
            container.alarmScheduler.rescheduleAll()
        }
    }
}
