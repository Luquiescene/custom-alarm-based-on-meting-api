package com.example.customalarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackIndexTest {
    @Test
    fun normalize_wrapsAndModulo() {
        assertEquals(0, TrackIndex.normalize(0, 5))
        assertEquals(2, TrackIndex.normalize(7, 5))
        assertEquals(4, TrackIndex.normalize(-1, 5))
        assertEquals(0, TrackIndex.normalize(3, 0))
    }
}
