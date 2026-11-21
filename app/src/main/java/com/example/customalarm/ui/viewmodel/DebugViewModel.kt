package com.example.customalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customalarm.data.local.RingLogEntity
import com.example.customalarm.di.AppContainer
import com.example.customalarm.util.CustomAlarmLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class DebugViewModel(private val container: AppContainer) : ViewModel() {

    val logs = container.alarmRepository.observeRingLogs(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<RingLogEntity>())

    fun scheduleTestRing(delayMs: Long = 10_000L) {
        container.alarmScheduler.scheduleTest(delayMs)
        CustomAlarmLog.i("已请求 10 秒后测试响铃")
    }
}
