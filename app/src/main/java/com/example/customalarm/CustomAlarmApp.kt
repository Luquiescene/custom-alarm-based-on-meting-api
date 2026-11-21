package com.example.customalarm

import android.app.Application
import com.example.customalarm.di.AppGraph
import com.example.customalarm.util.NotificationChannels

class CustomAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        NotificationChannels.create(this)
    }
}
