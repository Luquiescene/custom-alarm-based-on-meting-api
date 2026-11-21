package com.example.customalarm.data.download

/**
 * 音频内容判定：用于在解析音频直链/type=url 时识别音频响应。
 */
object AudioSniff {

    /** 判定 Content-Type 是否为音频（含常见的二进制流兜底）。 */
    fun isAudioContentType(contentType: String): Boolean {
        val ct = contentType.substringBefore(';').trim().lowercase()
        return ct.startsWith("audio/") ||
            ct == "application/octet-stream" ||
            ct == "binary/octet-stream"
    }
}
