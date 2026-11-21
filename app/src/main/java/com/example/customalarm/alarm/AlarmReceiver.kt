package com.example.customalarm.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.customalarm.ui.ringing.RingingActivity
import com.example.customalarm.ui.ringing.RingingContract
import com.example.customalarm.ui.ringing.RingingNotification
import com.example.customalarm.ui.ringing.RingingService
import com.example.customalarm.util.CustomAlarmLog
import com.example.customalarm.util.NotificationChannels

/**
 * 闹钟触发入口：
 * 1. 先直接拉起全屏 Activity（打开应用界面并点亮屏幕）；
 * 2. 再发全屏通知（普通 notify，锁屏下补充展示 + 供前台服务 startForeground 复用）；
 * 3. 启动前台响铃服务（持续播放 + 震动 + 唤醒锁）。
 * setAlarmClock 的精确闹钟属于前台服务后台启动的豁免场景，锁屏/进程被杀时仍能启动。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            handle(context.applicationContext, intent)
        } catch (e: Exception) {
            CustomAlarmLog.e("AlarmReceiver 处理失败", e)
        }
    }

    private fun handle(context: Context, intent: Intent) {
        val isTest = intent.action == RingingContract.ACTION_TEST
        val alarmId = intent.getLongExtra(RingingContract.EXTRA_ALARM_ID, -1L)
        val scheduledAt = intent.getLongExtra(RingingContract.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val now = System.currentTimeMillis()

        // 1. 先直接拉起全屏 Activity（打开应用界面）。全屏 intent 在 Android 14+ 侧载应用会被降级为 heads-up，
        //    这里优先依赖「显示在其他应用之上」权限的后台启动豁免直接 startActivity，先点亮并打开界面。
        try {
            val activityIntent = Intent(context, RingingActivity::class.java).apply {
                putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
                putExtra(RingingContract.EXTRA_IS_TEST, isTest)
                putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
                putExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, now)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            CustomAlarmLog.e("直接拉起全屏页失败", e)
        }

        // 2. 再发全屏通知（普通 notify），锁屏下补充展示，并供前台服务 startForeground 复用同一 ID。
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.notify(NotificationChannels.RINGING_NOTIFICATION_ID, RingingNotification.build(context, alarmId, isTest, scheduledAt, now))
            } catch (e: Exception) {
                CustomAlarmLog.e("发全屏通知失败", e)
            }
        }

        // 3. 启动前台响铃服务（持续播放 + 震动 + 唤醒锁）
        val serviceIntent = Intent(context, RingingService::class.java).apply {
            putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
            putExtra(RingingContract.EXTRA_IS_TEST, isTest)
            putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, now)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            CustomAlarmLog.e("启动响铃前台服务失败", e)
        }

        CustomAlarmLog.i("闹钟触发 alarmId=$alarmId scheduledAt=$scheduledAt actual=$now test=$isTest")
    }
}
