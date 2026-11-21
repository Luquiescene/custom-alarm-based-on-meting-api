package com.example.customalarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatRuleTest {
    @Test
    fun summary() {
        assertEquals("仅一次", RepeatRule.summary(emptySet()))
        assertEquals("工作日", RepeatRule.summary(setOf(1, 2, 3, 4, 5)))
        assertEquals("每天", RepeatRule.summary((1..7).toSet()))
        assertEquals("自定义", RepeatRule.summary(setOf(1, 2, 3)))
    }
}
