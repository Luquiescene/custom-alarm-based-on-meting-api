package com.example.customalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.customalarm.data.local.AlarmDao
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.prefs.SettingsRepository
import com.example.customalarm.domain.ScheduleCalculator
import com.example.customalarm.ui.MainActivity
import com.example.customalarm.ui.ringing.RingingContract
import com.example.customalarm.util.CustomAlarmLog

/** 统一使用 setAlarmClock 的调度器。 */
class AlarmScheduler(
    context: Context,
    private val alarmDao: AlarmDao,
    private val settingsRepository: SettingsRepository
) {
    private val appContext = context.applicationContext

    /** 重排全部：网易云校验未通过则全部取消（闸门）。 */
    suspend fun rescheduleAll() {
        val settings = settingsRepository.current()
        val gate = settings.apiValidated
        val all = alarmDao.getAll()
        for (alarm in all) {
            if (alarm.enabled && gate) schedule(alarm) else cancel(alarm.id)
        }
        CustomAlarmLog.i("rescheduleAll: gate=$gate, alarms=${all.size}")
    }

    suspend fun rescheduleOne(alarm: AlarmEntity) {
        val settings = settingsRepository.current()
        if (alarm.enabled && settings.apiValidated) schedule(alarm) else cancel(alarm.id)
    }

    private suspend fun schedule(alarm: AlarmEntity) {
        val next = ScheduleCalculator.nextTriggerMillis(
            alarm.hour, alarm.minute, alarm.repeatDays, System.currentTimeMillis()
        )
        if (next == null) {
            // 一次性闹钟已过时 → 自动关闭
            alarmDao.update(alarm.copy(enabled = false))
            cancel(alarm.id)
            CustomAlarmLog.i("一次性闹钟 ${alarm.id} 已过时，自动关闭")
            return
        }
        val am = appContext.getSystemService(AlarmManager::class.java)
        val info = AlarmManager.AlarmClockInfo(next, showPendingIntent())
        am.setAlarmClock(info, ringPendingIntent(alarm.id, next))
        CustomAlarmLog.i("已调度闹钟 ${alarm.id} -> $next")
    }

    fun cancel(alarmId: Long) {
        val am = appContext.getSystemService(AlarmManager::class.java)
        am.cancel(ringPendingIntent(alarmId, 0L))
    }

    /** 贪睡：在指定时刻重新调度（覆盖原计划）。 */
    fun scheduleAt(alarmId: Long, triggerAt: Long) {
        val am = appContext.getSystemService(AlarmManager::class.java)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent()), ringPendingIntent(alarmId, triggerAt))
        CustomAlarmLog.i("贪睡调度闹钟 ${alarmId} -> $triggerAt")
    }

    /** Debug 用：10 秒后测试响铃。 */
    fun scheduleTest(delayMs: Long = 10_000L) {
        val trigger = System.currentTimeMillis() + delayMs
        val am = appContext.getSystemService(AlarmManager::class.java)
        val intent = Intent(appContext, AlarmReceiver::class.java).apply { action = RingingContract.ACTION_TEST }
        val pi = PendingIntent.getBroadcast(
            appContext, TEST_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, showPendingIntent()), pi)
        CustomAlarmLog.i("已调度测试响铃 -> $trigger")
    }

    private fun ringPendingIntent(alarmId: Long, scheduledAt: Long): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = RingingContract.ACTION_RING
            putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
            putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            appContext, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            appContext, SHOW_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val SHOW_REQUEST_CODE = 0x1000
        private const val TEST_REQUEST_CODE = 9999
    }
}
