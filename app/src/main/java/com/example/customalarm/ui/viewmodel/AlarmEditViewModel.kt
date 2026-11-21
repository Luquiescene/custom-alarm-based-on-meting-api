package com.example.customalarm.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.local.PlaylistTrackEntity
import com.example.customalarm.data.net.ApiError
import com.example.customalarm.data.net.ApiResult
import com.example.customalarm.data.net.MetingApi
import com.example.customalarm.data.net.SongDto
import com.example.customalarm.data.prefs.SettingsSnapshot
import com.example.customalarm.data.prefs.resolveBaseUrl
import com.example.customalarm.di.AppContainer
import com.example.customalarm.domain.AlarmDefaults
import com.example.customalarm.domain.AudioUrl
import com.example.customalarm.domain.RingSource
import com.example.customalarm.util.CustomAlarmLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmEditViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsSnapshot())

    private var editingId: Long = -1L
    private var originalPlaylistId: String? = null
    private var originalSource: RingSource = RingSource.NETEASE

    var hour by mutableStateOf(7)
    var minute by mutableStateOf(0)
    var label by mutableStateOf("")
    var repeatDays by mutableStateOf(setOf<Int>())
    var enabled by mutableStateOf(false)
    var snoozeEnabled by mutableStateOf(true)
    var snoozeInterval by mutableStateOf(AlarmDefaults.SNOOZE_INTERVAL_MINUTES)
    var snoozeMax by mutableStateOf(AlarmDefaults.SNOOZE_MAX_COUNT)
    var ringDuration by mutableStateOf(AlarmDefaults.RING_DURATION_SECONDS)
    var vibrate by mutableStateOf(true)
    var volumeRamp by mutableStateOf(false)
    var ringSource by mutableStateOf(RingSource.NETEASE)
    var customUrl by mutableStateOf("")
    var playlistId by mutableStateOf("")
    var nextIndex by mutableStateOf(0)

    var existingTracks by mutableStateOf<List<PlaylistTrackEntity>>(emptyList())
    var previewSongs by mutableStateOf<List<SongDto>>(emptyList())
    var previewLoading by mutableStateOf(false)
    var saveError by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)

    fun load(id: Long) {
        if (id < 0) return
        editingId = id
        viewModelScope.launch {
            val alarm = container.alarmRepository.getById(id) ?: return@launch
            hour = alarm.hour
            minute = alarm.minute
            label = alarm.label
            repeatDays = alarm.repeatDays
            enabled = alarm.enabled
            snoozeEnabled = alarm.snoozeEnabled
            snoozeInterval = alarm.snoozeIntervalMinutes
            snoozeMax = alarm.snoozeMaxCount
            ringDuration = alarm.ringDurationSeconds
            vibrate = alarm.vibrate
            volumeRamp = alarm.volumeRamp
            ringSource = RingSource.from(alarm.ringSource)
            customUrl = alarm.customUrl ?: ""
            playlistId = alarm.playlistId ?: ""
            nextIndex = alarm.nextIndex
            originalPlaylistId = alarm.playlistId
            originalSource = RingSource.from(alarm.ringSource)
            existingTracks = container.alarmRepository.getTracks(id)
            if (existingTracks.isNotEmpty()) {
                previewSongs = existingTracks.map { SongDto(it.songId, it.title, it.artist, it.url) }
            }
        }
    }

    /** 预览歌单（曲目数 + 前 3 首标题）。 */
    fun fetchPreview() {
        val id = playlistId.trim()
        if (!isValidPlaylistId(id)) {
            previewSongs = emptyList()
            return
        }
        previewLoading = true
        viewModelScope.launch {
            try {
                val s = container.settingsRepository.current()
                val (base, server) = MetingApi.resolveSourceContext(ringSource.name, customUrl, resolveBaseUrl(s))
                val result = container.metingApi.fetchPlaylist(base, server, id)
                previewSongs = when (result) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> {
                        saveError = "歌单加载失败：${ApiError.describe(result.error)}"
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                saveError = "歌单加载异常"
            } finally {
                previewLoading = false
            }
        }
    }

    fun onPlaylistIdChange(value: String) { playlistId = value; clearPreview() }
    fun onRingSourceChange(value: RingSource) { ringSource = value; clearPreview() }
    fun onCustomUrlChange(value: String) { customUrl = value; clearPreview() }

    private fun clearPreview() {
        previewSongs = emptyList()
        saveError = null
    }

    fun save(onDone: () -> Unit) {
        if (saving) return
        saveError = null
        val source = ringSource
        val plId = playlistId.trim()

        // 基本校验
        if (source != RingSource.CUSTOM && !isValidPlaylistId(plId)) {
            saveError = "歌单 ID 非法（需 1–20 位数字）"
            return
        }
        if (source == RingSource.CUSTOM && plId.isNotEmpty() && !isValidPlaylistId(plId)) {
            saveError = "歌单 ID 非法（需 1–20 位数字）"
            return
        }

        saving = true
        viewModelScope.launch {
            try {
                val s = container.settingsRepository.current()
                val isDirect = source == RingSource.CUSTOM && AudioUrl.isDirect(customUrl.trim())

                // 直链 HEAD 校验
                if (isDirect) {
                    val head = container.metingApi.checkDirectAudioUrl(customUrl.trim())
                    if (head is ApiResult.Failure) {
                        saveError = "直链校验失败：${ApiError.describe(head.error)}"
                        saving = false
                        return@launch
                    }
                }

                // 歌单来源：保存时在线拉取歌单（不缓存音频）
                var songs: List<SongDto>? = null
                var trackServer = source.server ?: "netease"
                if (!isDirect) {
                    val ctx = MetingApi.resolveSourceContext(source.name, customUrl.trim(), resolveBaseUrl(s))
                    trackServer = ctx.second
                    when (val result = container.metingApi.fetchPlaylist(ctx.first, trackServer, plId)) {
                        is ApiResult.Failure -> {
                            saveError = "歌单加载失败：${ApiError.describe(result.error)}"
                            saving = false
                            return@launch
                        }
                        is ApiResult.Success -> {
                            if (result.value.isEmpty()) {
                                saveError = "歌单为空"
                                saving = false
                                return@launch
                            }
                            songs = result.value
                        }
                    }
                }

                // 歌单/来源变更 → 索引归零
                val playlistChanged = source != originalSource || plId != (originalPlaylistId ?: "")
                val finalNextIndex = if (playlistChanged) 0 else nextIndex

                val draft = AlarmEntity(
                    id = if (editingId >= 0) editingId else 0,
                    hour = hour,
                    minute = minute,
                    label = label.trim(),
                    repeatDays = repeatDays,
                    enabled = enabled && s.apiValidated,
                    snoozeEnabled = snoozeEnabled,
                    snoozeIntervalMinutes = snoozeInterval,
                    snoozeMaxCount = snoozeMax,
                    ringDurationSeconds = ringDuration,
                    vibrate = vibrate,
                    volumeRamp = volumeRamp,
                    ringSource = source.name,
                    customUrl = if (source == RingSource.CUSTOM) customUrl.trim() else null,
                    playlistId = if (isDirect) null else plId,
                    nextIndex = finalNextIndex
                )

                val savedId = if (editingId >= 0) {
                    container.alarmRepository.update(draft)
                    editingId
                } else {
                    container.alarmRepository.insert(draft)
                }
                val saved = container.alarmRepository.getById(savedId)
                    ?: throw IllegalStateException("保存后读取闹钟失败")

                // 落库曲目
                if (isDirect) {
                    container.playlistRepository.storeDirectUrl(saved, customUrl.trim())
                } else {
                    val tracks = songs!!.mapIndexed { i, song ->
                        PlaylistTrackEntity(saved.id, i, song.songId, trackServer, song.title, song.artist, song.url)
                    }
                    container.alarmRepository.replaceTracks(saved.id, tracks)
                }

                container.alarmScheduler.rescheduleOne(saved)
                container.alarmScheduler.rescheduleAll()
                onDone()
            } catch (e: Exception) {
                CustomAlarmLog.e("保存闹钟失败", e)
                saveError = "保存失败：${e.message ?: "未知错误"}"
            } finally {
                saving = false
            }
        }
    }

    private fun isValidPlaylistId(id: String): Boolean =
        id.isNotEmpty() && id.length in 1..20 && id.all { it.isDigit() }

    /** 自定义来源下，是否判定为音频直链（用于置灰歌单 ID）。 */
    fun isCustomDirect(): Boolean = ringSource == RingSource.CUSTOM && AudioUrl.isDirect(customUrl.trim())

    companion object {
        const val NEW_ALARM_ID = -1L
    }
}
