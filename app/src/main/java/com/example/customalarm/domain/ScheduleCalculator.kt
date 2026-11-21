package com.example.customalarm.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 闹钟下次触发时间计算。纯函数，便于单元测试。
 */
object ScheduleCalculator {

    /**
     * 计算下一次触发时刻（epoch millis）。
     * - repeatDays 为空：一次性，今天目标时间已过则顺延到次日同一时刻。
     * - repeatDays 非空：返回 [from] 之后最近的一个命中星期几与时分。
     */
    fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        fromMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val from = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zoneId)
        return nextTrigger(hour, minute, repeatDays, from)?.toInstant()?.toEpochMilli()
    }

    fun nextTrigger(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int>,
        from: ZonedDateTime
    ): ZonedDateTime? {
        val zone = from.zone
        val today = from.toLocalDate()

        if (repeatDays.isEmpty()) {
            val candidate = today.atTime(hour, minute).atZone(zone)
            return if (candidate.isAfter(from)) candidate
            else today.plusDays(1).atTime(hour, minute).atZone(zone)
        }

        // 最多向后找 8 天，必然覆盖任意重复组合。
        for (offset in 0..8L) {
            val day = today.plusDays(offset)
            val dow = day.dayOfWeek.value // 1=周一 … 7=周日
            if (dow in repeatDays) {
                val candidate = day.atTime(hour, minute).atZone(zone)
                if (candidate.isAfter(from)) return candidate
            }
        }
        return null
    }
}
