package com.example.customalarm.ui.ringing

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.local.PlaylistTrackEntity
import com.example.customalarm.data.local.RingLogEntity
import com.example.customalarm.data.net.ApiError
import com.example.customalarm.data.net.ApiResult
import com.example.customalarm.data.net.ApiValidation
import com.example.customalarm.data.net.MetingApi
import com.example.customalarm.data.prefs.ApiEndpoints
import com.example.customalarm.data.prefs.resolveBaseUrl
import com.example.customalarm.di.AppContainer
import com.example.customalarm.di.AppGraph
import com.example.customalarm.domain.AudioUrl
import com.example.customalarm.domain.RingSource
import com.example.customalarm.domain.TrackIndex
import com.example.customalarm.util.CustomAlarmLog
import com.example.customalarm.util.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 前台服务：响铃时负责持续播放 + 震动 + 唤醒锁 + 前台通知（全屏 intent 拉起 UI）。
 * 独立于 Activity，保证锁屏 / 后台进程被清时仍能响铃。
 */
class RingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player by lazy { RingtonePlayer(this) }
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoStopJob: Job? = null

    private var alarmId = -1L
    private var isTest = false
    private var scheduledAt = 0L
    private var actualTriggerAt = 0L
    private var alarm: AlarmEntity? = null
    private var tracks: List<PlaylistTrackEntity> = emptyList()
    private var ringDurationSeconds = 45

    private var playResult = "SILENT"
    private var errorMsg: String? = null
    private var isRinging = false
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(RingingContract.EXTRA_ACTION)
        alarmId = intent?.getLongExtra(RingingContract.EXTRA_ALARM_ID, -1L) ?: -1L
        isTest = intent?.getBooleanExtra(RingingContract.EXTRA_IS_TEST, false) ?: false
        scheduledAt = intent?.getLongExtra(RingingContract.EXTRA_SCHEDULED_AT, 0L) ?: 0L
        actualTriggerAt = intent?.getLongExtra(RingingContract.EXTRA_ACTUAL_TRIGGER_AT, 0L) ?: 0L

        when (action) {
            RingingContract.ACTION_STOP -> { finishRinging(snooze = false); return START_NOT_STICKY }
            RingingContract.ACTION_SNOOZE -> { finishRinging(snooze = true); return START_NOT_STICKY }
        }

        if (!isRinging) startRinging()
        return START_NOT_STICKY
    }

    private fun startRinging() {
        if (isRinging) return
        isRinging = true
        startForegroundCompat()
        acquireWakeLock()

        val container = AppGraph.container
        if (container == null) {
            CustomAlarmLog.e("RingingService: 容器为空")
            finishRinging(snooze = false)
            return
        }

        scope.launch {
            try {
                if (!isTest) {
                    alarm = container.alarmRepository.getById(alarmId)
                    tracks = container.alarmRepository.getTracks(alarmId)
                    ringDurationSeconds = alarm?.ringDurationSeconds ?: 45
                } else {
                    ringDurationSeconds = 45
                }
                beginPlayback(container)
                scheduleAutoStop()
            } catch (e: Exception) {
                CustomAlarmLog.e("开始响铃失败", e)
                errorMsg = "响铃启动失败，仅震动"
                if (shouldVibrate()) VibratorHelper.start(this@RingingService)
            }
        }
    }

    private fun shouldVibrate(): Boolean = if (isTest) true else (alarm?.vibrate ?: true)

    private suspend fun beginPlayback(container: AppContainer) {
        if (shouldVibrate()) VibratorHelper.start(this)
        playResult = if (shouldVibrate()) "VIBRATE_ONLY" else "SILENT"

        if (isTest) {
            playTestOnline(container)
        } else {
            val a = alarm ?: return
            val track = currentTrack(a)
            if (track == null) {
                errorMsg = "歌单无曲目"
            } else {
                playTrackOnline(container, a, track)
            }
        }
        CustomAlarmLog.i("响铃开始 alarmId=$alarmId playResult=$playResult")
    }

    private suspend fun playTrackOnline(container: AppContainer, a: AlarmEntity, track: PlaylistTrackEntity) {
        try {
            val url = resolveTrackUrl(container, a, track)
            if (url.isNullOrBlank()) {
                errorMsg = "无法解析音频地址"
                return
            }
            player.start(url, a.volumeRamp, scope) { }
            playResult = "PLAYED"
        } catch (e: Exception) {
            CustomAlarmLog.e("在线播放失败", e)
            errorMsg = "在线播放失败：${e.message}"
        }
    }

    private suspend fun resolveTrackUrl(container: AppContainer, a: AlarmEntity, track: PlaylistTrackEntity): String? {
        if (RingSource.from(a.ringSource) == RingSource.CUSTOM && AudioUrl.isDirect(a.customUrl.orEmpty())) {
            return a.customUrl
        }
        val s = container.settingsRepository.current()
        val (base, server) = MetingApi.resolveSourceContext(a.ringSource, a.customUrl, resolveBaseUrl(s))
        return when (val r = container.metingApi.resolveAudioUrl(base, server, track.songId)) {
            is ApiResult.Success -> r.value
            is ApiResult.Failure -> {
                CustomAlarmLog.w("解析曲目 ${track.songId} 音频失败：${ApiError.describe(r.error)}")
                null
            }
        }
    }

    private suspend fun playTestOnline(container: AppContainer) {
        try {
            val s = container.settingsRepository.current()
            val selected = resolveBaseUrl(s)
            val bases = buildList {
                if (selected.isNotBlank()) add(selected)
                ApiEndpoints.PRESETS.forEach { if (it != selected) add(it) }
            }
            var lastReason = "未知"
            for (base in bases) {
                when (val r = container.metingApi.resolveAudioUrl(base, "netease", ApiValidation.NON_VIP_NETEASE_SONG_ID)) {
                    is ApiResult.Success -> {
                        val url = r.value
                        if (!url.isNullOrBlank()) {
                            player.start(url, false, scope) { }
                            playResult = "PLAYED"
                            return
                        }
                        lastReason = "返回空地址"
                    }
                    is ApiResult.Failure -> lastReason = ApiError.describe(r.error)
                }
            }
            errorMsg = "测试：无法解析样本音频（$lastReason）"
        } catch (e: Exception) {
            CustomAlarmLog.e("测试在线播放失败", e)
            errorMsg = "测试在线播放异常：${e.message}"
        }
    }

    private fun currentTrack(a: AlarmEntity): PlaylistTrackEntity? {
        if (tracks.isEmpty()) return null
        val idx = TrackIndex.normalize(a.nextIndex, tracks.size)
        return tracks.getOrNull(idx)
    }

    private fun scheduleAutoStop() {
        autoStopJob?.cancel()
        autoStopJob = scope.launch {
            delay(ringDurationSeconds * 1000L)
            finishRinging(snooze = true)
        }
    }

    /** 结束响铃：先落库（贪睡/推进索引 + 响铃日志），再清理并停止。 */
    private fun finishRinging(snooze: Boolean) {
        if (finished) return
        finished = true
        scope.launch {
            try {
                val container = AppGraph.container
                if (!isTest && container != null) {
                    val a = container.alarmRepository.getById(alarmId) ?: alarm
                    if (a != null) {
                        val doSnooze = snooze && a.snoozeEnabled && a.snoozeCount < a.snoozeMaxCount
                        if (doSnooze) {
                            container.alarmRepository.incrementSnooze(a.id)
                            container.alarmScheduler.scheduleAt(a.id, System.currentTimeMillis() + a.snoozeIntervalMinutes * 60_000L)
                            CustomAlarmLog.i("贪睡闹钟 ${a.id}，次数=${a.snoozeCount + 1}")
                        } else {
                            val wasOneTime = a.repeatDays.isEmpty()
                            val updated = container.alarmRepository.advanceNextIndex(a.id)
                            if (wasOneTime) container.alarmRepository.markDisabled(a.id)
                            container.alarmScheduler.rescheduleOne(updated ?: a.copy(enabled = false))
                        }
                    }
                }
                if (container != null) {
                    container.alarmRepository.addRingLog(
                        RingLogEntity(
                            alarmId = alarmId,
                            scheduledAt = scheduledAt,
                            actualTriggerAt = actualTriggerAt,
                            trackTitle = currentTrackTitle(),
                            trackId = trackId(),
                            playResult = playResult,
                            error = errorMsg
                        )
                    )
                }
            } catch (e: Exception) {
                CustomAlarmLog.e("结束响铃处理失败", e)
            } finally {
                cleanupAndStopSelf()
            }
        }
    }

    private fun currentTrackTitle(): String {
        val a = alarm ?: return ""
        val t = currentTrack(a) ?: return ""
        return t.title
    }

    private fun trackId(): String? {
        val a = alarm ?: return null
        return currentTrack(a)?.songId
    }

    private fun cleanupAndStopSelf() {
        autoStopJob?.cancel()
        player.stop()
        VibratorHelper.stop(this)
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isRinging = false
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notification = RingingNotification.build(this, alarmId, isTest, scheduledAt, actualTriggerAt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationChannels.RINGING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationChannels.RINGING_NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        // SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP：熄屏时点亮屏幕（等价于已废弃的 FULL_WAKE_LOCK），
        // ON_AFTER_RELEASE：释放后短暂保持屏幕，避免闪烁。PARTIAL 只保 CPU 不亮屏，故这里必须用屏幕级唤醒锁。
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "customalarm:ringing"
        ).apply {
            setReferenceCounted(false)
            acquire(5 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        player.stop()
        VibratorHelper.stop(this)
        releaseWakeLock()
        super.onDestroy()
    }
}
