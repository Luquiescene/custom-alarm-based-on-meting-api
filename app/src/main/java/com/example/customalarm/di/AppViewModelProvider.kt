package com.example.customalarm.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.customalarm.ui.viewmodel.AlarmEditViewModel
import com.example.customalarm.ui.viewmodel.AlarmListViewModel
import com.example.customalarm.ui.viewmodel.DebugViewModel
import com.example.customalarm.ui.viewmodel.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { AlarmListViewModel(AppGraph.container!!) }
        initializer { AlarmEditViewModel(AppGraph.container!!) }
        initializer { SettingsViewModel(AppGraph.container!!) }
        initializer { DebugViewModel(AppGraph.container!!) }
    }
}
