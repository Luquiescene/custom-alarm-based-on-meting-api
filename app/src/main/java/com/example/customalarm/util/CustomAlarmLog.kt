package com.example.customalarm.util

import android.util.Log
import com.example.customalarm.BuildConfig

/** 统一日志：tag = CustomAlarm，release 关闭。 */
object CustomAlarmLog {
    const val TAG = "CustomAlarm"

    private val enabled: Boolean get() = BuildConfig.DEBUG

    fun d(msg: String) { if (enabled) Log.d(TAG, msg) }
    fun i(msg: String) { if (enabled) Log.i(TAG, msg) }
    fun w(msg: String) { if (enabled) Log.w(TAG, msg) }
    fun e(msg: String, t: Throwable? = null) { if (enabled) Log.e(TAG, msg, t) }
}
