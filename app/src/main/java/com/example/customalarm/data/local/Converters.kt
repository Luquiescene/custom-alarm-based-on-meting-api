package com.example.customalarm.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = value.sorted().joinToString(",")

    @TypeConverter
    fun toIntSet(value: String): Set<Int> =
        if (value.isBlank()) emptySet() else value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
}
