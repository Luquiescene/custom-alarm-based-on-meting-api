package com.example.customalarm.domain

/** 铃声来源。server 为 Meting 协议中的平台参数。 */
enum class RingSource(val server: String?, val label: String) {
    NETEASE("netease", "网易云音乐"),
    TENCENT("tencent", "QQ音乐"),
    KUGOU("kugou", "酷狗音乐"),
    CUSTOM(null, "自定义");

    companion object {
        fun from(value: String?): RingSource = entries.firstOrNull { it.name == value } ?: NETEASE
    }
}
