package com.example.customalarm.data.repository

import com.example.customalarm.data.local.AlarmDao
import com.example.customalarm.data.local.AlarmEntity
import com.example.customalarm.data.local.PlaylistDao
import com.example.customalarm.data.local.PlaylistTrackEntity
import com.example.customalarm.data.local.RingLogDao
import com.example.customalarm.data.local.RingLogEntity
import com.example.customalarm.domain.TrackIndex
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val playlistDao: PlaylistDao,
    private val ringLogDao: RingLogDao
) {
    fun observeAlarms(): Flow<List<AlarmEntity>> = alarmDao.observeAll()
    fun observeAlarm(id: Long): Flow<AlarmEntity?> = alarmDao.observeById(id)

    suspend fun getById(id: Long): AlarmEntity? = alarmDao.getById(id)
    suspend fun getAll(): List<AlarmEntity> = alarmDao.getAll()

    fun observeTracks(alarmId: Long): Flow<List<PlaylistTrackEntity>> = playlistDao.observeByAlarm(alarmId)
    suspend fun getTracks(alarmId: Long): List<PlaylistTrackEntity> = playlistDao.getByAlarm(alarmId)

    suspend fun insert(alarm: AlarmEntity): Long = alarmDao.insert(alarm)
    suspend fun update(alarm: AlarmEntity) = alarmDao.update(alarm)

    suspend fun delete(alarm: AlarmEntity) {
        alarmDao.delete(alarm)
        playlistDao.deleteByAlarm(alarm.id)
        ringLogDao.deleteByAlarm(alarm.id)
    }

    suspend fun replaceTracks(alarmId: Long, tracks: List<PlaylistTrackEntity>) {
        playlistDao.deleteByAlarm(alarmId)
        playlistDao.insertAll(tracks)
    }

    /** 响铃结束：nextIndex +1 取模并清零贪睡计数。 */
    suspend fun advanceNextIndex(alarmId: Long): AlarmEntity? {
        val alarm = alarmDao.getById(alarmId) ?: return null
        val count = playlistDao.getByAlarm(alarmId).size
        val updated = alarm.copy(nextIndex = TrackIndex.normalize(alarm.nextIndex + 1, count), snoozeCount = 0)
        alarmDao.update(updated)
        return updated
    }

    suspend fun incrementSnooze(alarmId: Long): AlarmEntity? {
        val alarm = alarmDao.getById(alarmId) ?: return null
        val updated = alarm.copy(snoozeCount = alarm.snoozeCount + 1)
        alarmDao.update(updated)
        return updated
    }

    suspend fun markDisabled(alarmId: Long) {
        val alarm = alarmDao.getById(alarmId) ?: return
        alarmDao.update(alarm.copy(enabled = false))
    }

    // ---- 响铃日志 ----
    suspend fun addRingLog(log: RingLogEntity) = ringLogDao.insert(log)
    fun observeRingLogs(limit: Int): Flow<List<RingLogEntity>> = ringLogDao.observeRecent(limit)
    suspend fun getRecentLogs(): List<RingLogEntity> = ringLogDao.getRecent20()
}
