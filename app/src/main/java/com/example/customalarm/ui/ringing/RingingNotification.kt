package com.example.customalarm.ui.ringing

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.example.customalarm.R
import com.example.customalarm.util.NotificationChannels

/**
 * 响铃通知构建器：全屏 intent 拉起 [RingingActivity]，停止/贪睡动作发给 [RingingService]。
 * 由 AlarmReceiver（普通 notify，保证全屏 intent 及时触发）与 RingingService（startForeground 复用同一 ID）共用。
 */
object RingingNotification {
    private const val FS_REQUEST_CODE = 0x2000

    fun build(
        context: Context,
        alarmId: Long,
        isTest: Boolean,
        scheduledAt: Long,
        actualTriggerAt: Long,
        label: String? = null
    ): Notification {
        val text = if (isTest) "测试响铃" else (label?.ifBlank { null } ?: "闹钟响铃")
        val builder = Notification.Builder(context, NotificationChannels.RINGING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notification_ringing_title))
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi(context, alarmId, isTest, scheduledAt, actualTriggerAt), true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stop),
                    context.getString(R.string.notification_stop),
                    serviceActionPi(context, FS_REQUEST_CODE + 1, RingingContract.ACTION_STOP, alarmId, isTest, scheduledAt, actualTriggerAt)
                ).build()
            )
        val compactViewActions = mutableListOf(0)
        if (!isTest) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_snooze),
                    context.getString(R.string.notification_snooze),
                    serviceActionPi(context, FS_REQUEST_CODE + 2, RingingContract.ACTION_SNOOZE, alarmId, isTest, scheduledAt, actualTriggerAt)
                ).build()
            )
            compactViewActions.add(1)
        }
        builder.setStyle(Notification.MediaStyle().setShowActionsInCompactView(*compactViewActions.toIntArray()))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun fullScreenPi(
        context: Context,
        alarmId: Long,
        isTest: Boolean,
        scheduledAt: Long,
        actualTriggerAt: Long
    ): PendingIntent {
        val intent = Intent(context, RingingActivity::class.java).apply {
            putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
            putExtra(RingingContract.EXTRA_IS_TEST, isTest)
            putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, actualTriggerAt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(context, FS_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun serviceActionPi(
        context: Context,
        code: Int,
        action: String,
        alarmId: Long,
        isTest: Boolean,
        scheduledAt: Long,
        actualTriggerAt: Long
    ): PendingIntent {
        val intent = Intent(context, RingingService::class.java).apply {
            putExtra(RingingContract.EXTRA_ACTION, action)
            putExtra(RingingContract.EXTRA_ALARM_ID, alarmId)
            putExtra(RingingContract.EXTRA_IS_TEST, isTest)
            putExtra(RingingContract.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, actualTriggerAt)
        }
        return PendingIntent.getService(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
