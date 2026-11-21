package com.example.customalarm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RingLogDao {
    @Insert
    suspend fun insert(log: RingLogEntity)

    @Query("SELECT * FROM ring_logs ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RingLogEntity>>

    @Query("SELECT * FROM ring_logs ORDER BY createdAt DESC LIMIT 20")
    suspend fun getRecent20(): List<RingLogEntity>

    @Query("DELETE FROM ring_logs WHERE alarmId = :alarmId")
    suspend fun deleteByAlarm(alarmId: Long)
}
