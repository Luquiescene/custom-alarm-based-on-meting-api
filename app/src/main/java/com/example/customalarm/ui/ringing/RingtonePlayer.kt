package com.example.customalarm.ui.ringing

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Media3 播放器封装：USAGE_ALARM + 音量渐强 + 在线流播放。音频焦点交给 ExoPlayer 自动管理。 */
class RingtonePlayer(private val context: Context) {

    private var player: ExoPlayer? = null

    private val media3Attributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_ALARM)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    fun start(url: String, volumeRamp: Boolean, scope: CoroutineScope, onComplete: () -> Unit) {
        stop()
        val p = ExoPlayer.Builder(context).build().apply {
            // USAGE_ALARM 不支持自动音频焦点（仅 USAGE_MEDIA/USAGE_GAME 支持），必须传 false。
            setAudioAttributes(media3Attributes, false)
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onComplete()
                }
            })
        }
        player = p
        if (volumeRamp) rampVolume(p, scope) else p.volume = 1f
    }

    private fun rampVolume(p: ExoPlayer, scope: CoroutineScope) {
        p.volume = 0.1f
        scope.launch {
            val steps = 30
            for (i in 1..steps) {
                delay(300)
                p.volume = (0.1f + 0.9f * i / steps.toFloat()).coerceAtMost(1f)
            }
        }
    }

    fun stop() {
        player?.release()
        player = null
    }
}
