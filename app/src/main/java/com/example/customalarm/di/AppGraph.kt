package com.example.customalarm.di

import android.content.Context

/** 供 Receiver / Worker 等无构造注入的组件访问的全局容器。 */
object AppGraph {
    @Volatile
    var container: AppContainer? = null
        private set

    fun init(context: Context) {
        if (container == null) {
            synchronized(this) {
                if (container == null) container = AppContainer(context)
            }
        }
    }
}
