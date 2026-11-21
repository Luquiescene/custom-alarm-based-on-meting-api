package com.example.customalarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.customalarm.domain.AlarmDefaults

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = false,
    val snoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = AlarmDefaults.SNOOZE_INTERVAL_MINUTES,
    val snoozeMaxCount: Int = AlarmDefaults.SNOOZE_MAX_COUNT,
    val ringDurationSeconds: Int = AlarmDefaults.RING_DURATION_SECONDS,
    val vibrate: Boolean = true,
    val volumeRamp: Boolean = false,
    val ringSource: String = "netease",
    val customUrl: String? = null,
    val playlistId: String? = null,
    val nextIndex: Int = 0,
    val snoozeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
