package com.example.customalarm.data.prefs

import kotlinx.serialization.Serializable

/** 单个平台的能力探测结果。 */
@Serializable
data class CapabilityEntry(
    val server: String,
    val passed: Boolean,
    val reason: String? = null,
    val latencyMs: Long? = null,
    /** null=未探测；true/false=该平台 type=url 能否解析出音频（用非VIP样本）。 */
    val urlResolvable: Boolean? = null
)

@Serializable
data class SettingsSnapshot(
    val themeMode: String = ThemeMode.SYSTEM.name,
    val apiSelectedIndex: Int = 1,
    val apiCustomUrl: String = "",
    val apiValidated: Boolean = false,
    val validatedBase: String? = null,
    val capabilityMatrix: List<CapabilityEntry> = emptyList(),
    val onboardingShown: Boolean = false
)

object ApiEndpoints {
    val PRESETS = listOf(
        "https://api.i-meto.com/meting/api",
        "https://api.injahow.cn/meting/",
        "https://meting-ve.2333332.xyz/api"
    )
}

/** 根据设置快照解析当前生效的 API base URL。 */
fun resolveBaseUrl(s: SettingsSnapshot): String =
    if (s.apiSelectedIndex in ApiEndpoints.PRESETS.indices) ApiEndpoints.PRESETS[s.apiSelectedIndex]
    else s.apiCustomUrl.trim()
