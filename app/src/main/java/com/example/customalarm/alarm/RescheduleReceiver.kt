package com.example.customalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.customalarm.di.AppGraph
import com.example.customalarm.util.CustomAlarmLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 开机 / 时间变化 / 时区变化 / 应用更新后重排。 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val container = AppGraph.container ?: return@launch
                container.alarmScheduler.rescheduleAll()
                CustomAlarmLog.i("RescheduleReceiver: ${intent.action}")
            } catch (e: Exception) {
                CustomAlarmLog.e("RescheduleReceiver 失败", e)
            } finally {
                pending.finish()
            }
        }
    }
}
