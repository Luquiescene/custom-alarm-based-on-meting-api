package com.example.customalarm.data.repository

import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.local.PlaylistTrackEntity
import com.example.customalarm.data.net.ApiResult
import com.example.customalarm.data.net.MetingApi
import com.example.customalarm.data.net.SongDto
import com.example.customalarm.data.prefs.SettingsRepository
import com.example.customalarm.data.prefs.resolveBaseUrl
import com.example.customalarm.domain.RingSource

class PlaylistRepository(
    private val api: MetingApi,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository
) {
    /** 拉取歌单并写入缓存表；失败时返回错误。 */
    suspend fun fetchAndStore(alarm: AlarmEntity, playlistId: String): ApiResult<List<SongDto>> {
        val settings = settingsRepository.current()
        val source = RingSource.from(alarm.ringSource)
        val base = resolveBaseUrl(settings)
        val server = source.server ?: "netease"
        val result = api.fetchPlaylist(base, server, playlistId)
        if (result is ApiResult.Success) {
            val tracks = result.value.mapIndexed { i, song ->
                PlaylistTrackEntity(
                    alarmId = alarm.id,
                    position = i,
                    songId = song.songId,
                    server = server,
                    title = song.title,
                    artist = song.artist,
                    url = song.url
                )
            }
            alarmRepository.replaceTracks(alarm.id, tracks)
        }
        return result
    }

    /** 直接保存自定义直链为单曲歌单。 */
    suspend fun storeDirectUrl(alarm: AlarmEntity, url: String) {
        val track = PlaylistTrackEntity(
            alarmId = alarm.id,
            position = 0,
            songId = "custom",
            server = "custom",
            title = alarm.label.ifBlank { "自定义铃声" },
            artist = "",
            url = url
        )
        alarmRepository.replaceTracks(alarm.id, listOf(track))
    }
}
