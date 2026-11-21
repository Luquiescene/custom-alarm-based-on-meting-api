package com.example.customalarm.ui.ringing

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

object VibratorHelper {
    fun start(context: Context) {
        val v = context.getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0))
    }

    fun stop(context: Context) {
        context.getSystemService(Vibrator::class.java)?.cancel()
    }
}
