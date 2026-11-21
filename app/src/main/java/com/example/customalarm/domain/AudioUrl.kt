package com.example.customalarm.domain

/** 自定义来源的 URL 判定。 */
object AudioUrl {
    val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".aac", ".ogg", ".flac")

    /** 判断是否音频直链；无法判定时按直链处理。 */
    fun isDirect(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (AUDIO_EXTENSIONS.any { lower.substringBefore('?').endsWith(it) }) return true
        val hasMeting = lower.contains("type=url") || lower.contains("server=") || lower.contains("type=playlist")
        return !hasMeting
    }
}
