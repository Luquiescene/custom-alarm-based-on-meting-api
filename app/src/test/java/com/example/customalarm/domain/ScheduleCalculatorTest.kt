package com.example.customalarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val daily: Set<Int> = (1..7).toSet()
    private val workdays: Set<Int> = setOf(1, 2, 3, 4, 5)

    // 2026-11-16 是周一（已验证）
    private fun mondayMorning(hour: Int) = ZonedDateTime.of(2026, 11, 16, hour, 0, 0, 0, zone)

    @Test
    fun daily_nextDayWhenTodayPassed() {
        val from = mondayMorning(8)
        val result = ScheduleCalculator.nextTrigger(7, 0, daily, from)
        assertEquals(ZonedDateTime.of(2026, 11, 17, 7, 0, 0, 0, zone), result)
    }

    @Test
    fun daily_todayWhenFuture() {
        val from = mondayMorning(6)
        val result = ScheduleCalculator.nextTrigger(7, 0, daily, from)
        assertEquals(ZonedDateTime.of(2026, 11, 16, 7, 0, 0, 0, zone), result)
    }

    @Test
    fun oneTime_future() {
        val result = ScheduleCalculator.nextTrigger(7, 0, emptySet(), mondayMorning(6))
        assertEquals(ZonedDateTime.of(2026, 11, 16, 7, 0, 0, 0, zone), result)
    }

    @Test
    fun oneTime_passed_returnsTomorrow() {
        val result = ScheduleCalculator.nextTrigger(7, 0, emptySet(), mondayMorning(8))
        assertEquals(ZonedDateTime.of(2026, 11, 17, 7, 0, 0, 0, zone), result)
    }

    @Test
    fun workdays_fridayEvening_skipsWeekend() {
        val friday = ZonedDateTime.of(2026, 11, 20, 8, 0, 0, 0, zone) // 周五
        val result = ScheduleCalculator.nextTrigger(7, 0, workdays, friday)
        assertEquals(ZonedDateTime.of(2026, 11, 23, 7, 0, 0, 0, zone), result) // 下周一
    }

    @Test
    fun workdays_fridayMorning_sameDay() {
        val friday = ZonedDateTime.of(2026, 11, 20, 6, 0, 0, 0, zone)
        val result = ScheduleCalculator.nextTrigger(7, 0, workdays, friday)
        assertEquals(ZonedDateTime.of(2026, 11, 20, 7, 0, 0, 0, zone), result)
    }

    @Test
    fun singleDay_weekly() {
        // 只选周六(6)
        val from = mondayMorning(6)
        val result = ScheduleCalculator.nextTrigger(9, 30, setOf(6), from)
        assertEquals(ZonedDateTime.of(2026, 11, 21, 9, 30, 0, 0, zone), result)
    }
}
