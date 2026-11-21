package com.example.customalarm.data.net

import com.example.customalarm.data.download.AudioSniff
import com.example.customalarm.domain.AudioUrl
import com.example.customalarm.domain.RingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder

class MetingApi(private val client: OkHttpClient) {

    /** type=playlist 拉取歌单。 */
    suspend fun fetchPlaylist(baseUrl: String, server: String, playlistId: String): ApiResult<List<SongDto>> =
        getBody(buildUrl(baseUrl, mapOf("server" to server, "type" to "playlist", "id" to playlistId))).flatMap { body ->
            if (!MetingParser.isJsonArray(body)) ApiResult.Failure(ApiError.NotJson)
            else ApiResult.Success(MetingParser.parseSongs(body))
        }

    /** type=song 单曲（用于能力探测）。 */
    suspend fun fetchSong(baseUrl: String, server: String, songId: String): ApiResult<List<SongDto>> =
        getBody(buildUrl(baseUrl, mapOf("server" to server, "type" to "song", "id" to songId))).flatMap { body ->
            if (!MetingParser.isJsonArray(body)) ApiResult.Failure(ApiError.NotJson)
            else ApiResult.Success(MetingParser.parseSongs(body))
        }

    /** type=url 取直链（兼容 JSON 与 302 跳转音频两种实现）。 */
    suspend fun fetchSongUrl(baseUrl: String, server: String, songId: String): ApiResult<String?> =
        resolveUrl(buildUrl(baseUrl, mapOf("server" to server, "type" to "url", "id" to songId)))

    /** 自定义 Meting 地址（url 模板）取直链。 */
    suspend fun fetchSongUrlFromTemplate(template: String, songId: String): ApiResult<String?> =
        resolveUrl(template.trimEnd() + songId)

    /** 解析某歌曲的真实音频地址（优先走 type=url）。 */
    suspend fun resolveAudioUrl(baseUrl: String, server: String, songId: String): ApiResult<String?> =
        fetchSongUrl(baseUrl, server, songId)

    /**
     * 解析 type=url 响应：标准 Meting 返回 JSON [{"url":"…"}]；
     * 部分实例（injahow/meting-ve）直接 302 跳到音频 CDN。
     * 两种都解析出真实音频地址；空/HTML（版权受限）返回 NotAudio。
     */
    private suspend fun resolveUrl(url: String): ApiResult<String?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(req).execute().use { resp ->
                when {
                    resp.code == 401 || resp.code == 403 -> return@withContext ApiResult.Failure(ApiError.Forbidden)
                    !resp.isSuccessful -> return@withContext ApiResult.Failure(ApiError.Http(resp.code))
                }
                val ct = resp.header("Content-Type") ?: ""
                if (AudioSniff.isAudioContentType(ct)) {
                    // 302 已追到音频 CDN：最终地址即真实音频
                    ApiResult.Success(resp.request.url.toString())
                } else {
                    val body = resp.body?.string() ?: return@withContext ApiResult.Failure(ApiError.Empty)
                    if (MetingParser.isJsonArray(body)) ApiResult.Success(MetingParser.parseFirstUrl(body))
                    else ApiResult.Failure(ApiError.NotAudio(ct.ifBlank { "无 Content-Type" }))
                }
            }
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message))
        }
    }

    /** 自定义 Meting 地址拉取歌单（重建 type=playlist 请求）。 */
    suspend fun fetchPlaylistFromTemplate(template: String, playlistId: String): ApiResult<List<SongDto>> {
        val base = templateBase(template)
        val server = templateServer(template) ?: "netease"
        return fetchPlaylist(base, server, playlistId)
    }

    /** 音频直链 HEAD 校验 Content-Type 是否为 audio 类型。 */
    suspend fun checkDirectAudioUrl(url: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).head().build()
            client.newCall(req).execute().use { resp ->
                val ct = resp.header("Content-Type") ?: ""
                when {
                    !resp.isSuccessful -> ApiResult.Failure(ApiError.Http(resp.code))
                    AudioSniff.isAudioContentType(ct) -> ApiResult.Success(true)
                    else -> ApiResult.Failure(ApiError.NotAudio(ct.ifBlank { "无 Content-Type" }))
                }
            }
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message))
        }
    }

    private suspend fun getBody(url: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext ApiResult.Failure(ApiError.Empty)
                when {
                    resp.code == 403 -> ApiResult.Failure(ApiError.Forbidden)
                    !resp.isSuccessful -> ApiResult.Failure(ApiError.Http(resp.code))
                    else -> ApiResult.Success(body)
                }
            }
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message))
        }
    }

    private inline fun <T, R> ApiResult<T>.flatMap(transform: (T) -> ApiResult<R>): ApiResult<R> =
        when (this) {
            is ApiResult.Failure -> ApiResult.Failure(error)
            is ApiResult.Success -> transform(value)
        }

    companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        fun buildUrl(base: String, params: Map<String, String>): String {
            val b = base.trim()
            val sep = if (b.contains("?")) "&" else "?"
            val qs = params.entries.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
            return b + sep + qs
        }

        fun urlEncode(v: String): String = URLEncoder.encode(v, "UTF-8")

        /** 从自定义模板中取 base（scheme://host/path，去掉 query）。 */
        fun templateBase(template: String): String {
            val t = template.trim()
            val qIdx = t.indexOf('?')
            return if (qIdx >= 0) t.substring(0, qIdx) else t
        }

        /** 从自定义模板 query 中解析 server。 */
        fun templateServer(template: String): String? {
            val t = template.trim()
            val qIdx = t.indexOf('?')
            if (qIdx < 0) return null
            val q = t.substring(qIdx + 1)
            for (pair in q.split('&')) {
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2 && kv[0].trim() == "server") return kv[1].trim()
            }
            return null
        }

        /**
         * 由来源 + 自定义 URL 解析出 (base, server)。
         * 自定义 Meting 模板（非直链）优先用模板自身的 base/server；
         * 其余（netease/tencent/kugou/直链）用全局 API base + 来源 server。
         */
        fun resolveSourceContext(sourceName: String, customUrl: String?, settingsBase: String): Pair<String, String> {
            val source = RingSource.from(sourceName)
            val cu = customUrl?.trim().orEmpty()
            return if (source == RingSource.CUSTOM && cu.isNotBlank() && !AudioUrl.isDirect(cu)) {
                templateBase(cu) to (templateServer(cu) ?: "netease")
            } else {
                settingsBase to (source.server ?: "netease")
            }
        }
    }
}
