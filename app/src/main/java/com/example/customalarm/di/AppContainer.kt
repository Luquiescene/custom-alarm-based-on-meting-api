package com.example.customalarm.di

import android.content.Context
import com.example.customalarm.alarm.AlarmScheduler
import com.example.customalarm.data.local.AppDatabase
import com.example.customalarm.data.net.ApiValidation
import com.example.customalarm.data.net.MetingApi
import com.example.customalarm.data.prefs.SettingsRepository
import com.example.customalarm.data.repository.AlarmRepository
import com.example.customalarm.data.repository.PlaylistRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** 手写依赖注入容器。 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.get(appContext)

    private val apiClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val settingsRepository = SettingsRepository(appContext)
    val metingApi = MetingApi(apiClient)
    val apiValidation = ApiValidation(metingApi)
    val alarmRepository = AlarmRepository(database.alarmDao(), database.playlistDao(), database.ringLogDao())
    val playlistRepository = PlaylistRepository(metingApi, settingsRepository, alarmRepository)
    val alarmScheduler = AlarmScheduler(appContext, database.alarmDao(), settingsRepository)
}
