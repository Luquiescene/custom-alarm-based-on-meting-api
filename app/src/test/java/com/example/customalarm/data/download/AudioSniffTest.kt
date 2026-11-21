package com.example.customalarm.data.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSniffTest {

    @Test
    fun isAudioContentType_audio() {
        assertTrue(AudioSniff.isAudioContentType("audio/mpeg"))
        assertTrue(AudioSniff.isAudioContentType("audio/x-m4a"))
        assertTrue(AudioSniff.isAudioContentType("audio/mpeg; charset=utf-8"))
    }

    @Test
    fun isAudioContentType_octetStream() {
        assertTrue(AudioSniff.isAudioContentType("application/octet-stream"))
        assertTrue(AudioSniff.isAudioContentType("binary/octet-stream"))
    }

    @Test
    fun isAudioContentType_nonAudio() {
        assertFalse(AudioSniff.isAudioContentType("text/html"))
        assertFalse(AudioSniff.isAudioContentType("image/jpeg"))
        assertFalse(AudioSniff.isAudioContentType(""))
    }
}
