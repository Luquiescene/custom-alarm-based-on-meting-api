package com.example.customalarm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist_tracks WHERE alarmId = :alarmId ORDER BY position")
    fun observeByAlarm(alarmId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE alarmId = :alarmId ORDER BY position")
    suspend fun getByAlarm(alarmId: Long): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE alarmId = :alarmId")
    suspend fun deleteByAlarm(alarmId: Long)
}
