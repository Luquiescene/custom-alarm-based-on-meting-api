package com.example.customalarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ring_logs")
data class RingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val scheduledAt: Long,
    val actualTriggerAt: Long,
    val trackTitle: String = "",
    val trackId: String? = null,
    val playResult: String,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
