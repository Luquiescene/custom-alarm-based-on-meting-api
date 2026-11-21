package com.example.customalarm.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["alarmId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("alarmId")]
)
data class PlaylistTrackEntity(
    val alarmId: Long,
    val position: Int,
    val songId: String,
    val server: String,
    val title: String,
    val artist: String,
    val url: String?,
    val cached: Boolean = false
)
