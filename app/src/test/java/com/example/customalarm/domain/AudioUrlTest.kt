package com.example.customalarm.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioUrlTest {
    @Test
    fun direct_audio_extensions() {
        assertTrue(AudioUrl.isDirect("https://x.com/a.mp3"))
        assertTrue(AudioUrl.isDirect("https://x.com/a.m4a?token=1"))
        assertTrue(AudioUrl.isDirect("https://x.com/a.flac"))
    }

    @Test
    fun meting_template_not_direct() {
        assertFalse(AudioUrl.isDirect("https://x.com/api?server=netease&type=url&id="))
        assertFalse(AudioUrl.isDirect("https://x.com/api?server=tencent&type=playlist&id=1"))
    }

    @Test
    fun unknown_treated_as_direct() {
        assertTrue(AudioUrl.isDirect("https://x.com/stream"))
    }
}
