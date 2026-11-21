package com.example.customalarm.data.net

/** 网络/解析错误分类，用于给用户不同提示。 */
sealed class ApiError(val message: String) {
    class Network(msg: String?) : ApiError(msg ?: "网络不可达")
    object Timeout : ApiError("请求超时")
    object Forbidden : ApiError("403 被拒绝")
    object Empty : ApiError("返回空结果")
    object NotJson : ApiError("返回非 JSON 数组")
    class Http(code: Int) : ApiError("HTTP $code")
    class NotAudio(ct: String) : ApiError("非音频类型：$ct")
    class Unknown(msg: String?) : ApiError(msg ?: "未知错误")

    companion object {
        fun describe(error: ApiError): String = when (error) {
            is Network -> "网络不可达，请检查网络"
            is Timeout -> "请求超时（10 秒）"
            is Forbidden -> "服务器拒绝访问（403）"
            is Empty -> "返回为空"
            is NotJson -> "返回格式不是 JSON 数组"
            is Http -> "请求失败（${error.message}）"
            is NotAudio -> "链接不是音频（${error.message}）"
            is Unknown -> "未知错误"
        }
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
}
