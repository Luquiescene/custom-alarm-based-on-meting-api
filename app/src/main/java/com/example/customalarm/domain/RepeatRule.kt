package com.example.customalarm.domain

/**
 * 重复规则。使用 ISO 星期编号：1=周一 … 7=周日；空集合表示"仅一次"。
 */
object RepeatRule {
    val WEEKDAYS: Set<Int> = setOf(1, 2, 3, 4, 5)
    val WEEKEND: Set<Int> = setOf(6, 7)
    val EVERY_DAY: Set<Int> = (1..7).toSet()

    val ALL_DAYS: List<Int> = (1..7).toList()

    fun summary(days: Set<Int>): String {
        if (days.isEmpty()) return "仅一次"
        return when (days) {
            WEEKDAYS -> "工作日"
            EVERY_DAY -> "每天"
            else -> "自定义"
        }
    }

    fun weekdayName(day: Int): String = when (day) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> ""
    }
}
