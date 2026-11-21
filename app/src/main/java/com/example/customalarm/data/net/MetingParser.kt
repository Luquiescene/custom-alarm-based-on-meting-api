package com.example.customalarm.data.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

data class SongDto(
    val songId: String,
    val title: String,
    val artist: String,
    val url: String?
)

/**
 * Meting 协议解析：兼容 name/artist 与 title/author 两套字段，
 * 非 JSON 数组一律按空列表处理，url 为 null 视为不可用。
 */
object MetingParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isJsonArray(body: String): Boolean =
        runCatching { json.parseToJsonElement(body) is JsonArray }.getOrDefault(false)

    fun parseSongs(body: String): List<SongDto> {
        val arr = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            val id = o.string("id") ?: o.string("song_id") ?: o.string("songid")
                ?: o.string("url")?.let { extractQueryParam(it, "id") }
                ?: return@mapNotNull null
            SongDto(
                songId = id,
                title = o.string("name") ?: o.string("title") ?: "",
                artist = o.string("artist") ?: o.string("author") ?: "",
                url = o.string("url")
            )
        }
    }

    /** 从 type=url 返回中取第一个非空 url。 */
    fun parseFirstUrl(body: String): String? {
        val arr = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return null
        for (el in arr) {
            val o = runCatching { el.jsonObject }.getOrNull() ?: continue
            val u = o.string("url")
            if (!u.isNullOrBlank()) return u
        }
        return null
    }

    private fun JsonObject.string(vararg keys: String): String? {
        for (k in keys) {
            when (val v = this[k]) {
                null -> Unit
                is JsonNull -> return null
                is JsonPrimitive -> return v.content
                else -> Unit
            }
        }
        return null
    }

    /** 从 URL 查询串解析指定参数（如 id=186016）。 */
    private fun extractQueryParam(url: String, key: String): String? {
        val qIdx = url.indexOf('?')
        if (qIdx < 0) return null
        val query = url.substring(qIdx + 1)
        for (pair in query.split('&')) {
            val kv = pair.split('=', limit = 2)
            if (kv.size == 2 && kv[0] == key) return kv[1].takeIf { it.isNotBlank() }
        }
        return null
    }
}
