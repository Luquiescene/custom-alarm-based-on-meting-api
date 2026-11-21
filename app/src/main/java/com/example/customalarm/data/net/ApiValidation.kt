package com.example.customalarm.data.net

import com.example.customalarm.data.prefs.CapabilityEntry

/** 能力探测：必测 netease，可选 tencent/kugou；netease 额外探测 type=url 能否解析出音频。 */
class ApiValidation(private val api: MetingApi) {

    suspend fun probe(baseUrl: String): List<CapabilityEntry> {
        val neteaseMeta = probePlatform(baseUrl, "netease", songId = "186016", fallbackPlaylistId = "60198")
        val neteaseUrlOk = probeUrl(baseUrl, "netease")
        val netease = neteaseMeta.copy(urlResolvable = neteaseUrlOk)
        val tencent = probePlatform(baseUrl, "tencent", songId = "002Mxf5z4z4z4p", fallbackPlaylistId = null)
        val kugou = probePlatform(baseUrl, "kugou", songId = "2x1x", fallbackPlaylistId = null)
        return listOf(netease, tencent, kugou)
    }

    private suspend fun probePlatform(
        baseUrl: String,
        server: String,
        songId: String,
        fallbackPlaylistId: String?
    ): CapabilityEntry {
        val start = System.currentTimeMillis()
        val song = api.fetchSong(baseUrl, server, songId)
        if (song is ApiResult.Success && song.value.isNotEmpty()) {
            return CapabilityEntry(server, true, null, System.currentTimeMillis() - start)
        }
        if (fallbackPlaylistId != null) {
            val pl = api.fetchPlaylist(baseUrl, server, fallbackPlaylistId)
            if (pl is ApiResult.Success && pl.value.isNotEmpty()) {
                return CapabilityEntry(server, true, null, System.currentTimeMillis() - start)
            }
            val err = when {
                song is ApiResult.Failure -> song.error
                pl is ApiResult.Failure -> pl.error
                else -> ApiError.Empty
            }
            return CapabilityEntry(server, false, ApiError.describe(err), System.currentTimeMillis() - start)
        }
        val err = (song as? ApiResult.Failure)?.error ?: ApiError.Empty
        return CapabilityEntry(server, false, ApiError.describe(err), System.currentTimeMillis() - start)
    }

    /** 用非VIP样本探测 type=url 能否解析出真实音频地址；不支持的平台返回 null。 */
    private suspend fun probeUrl(baseUrl: String, server: String): Boolean? {
        val sample = when (server) {
            "netease" -> NON_VIP_NETEASE_SONG_ID
            else -> return null
        }
        return when (val r = api.resolveAudioUrl(baseUrl, server, sample)) {
            is ApiResult.Success -> !r.value.isNullOrBlank()
            is ApiResult.Failure -> false
        }
    }

    companion object {
        /** 网易云非VIP样本曲目 id（type=song 元数据样本 186016 是 VIP，仅用于元数据探测）。 */
        const val NON_VIP_NETEASE_SONG_ID = "2755496359"
    }
}
