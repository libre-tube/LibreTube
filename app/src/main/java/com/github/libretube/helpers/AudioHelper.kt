package com.github.libretube.helpers

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.media.AudioManagerCompat
import com.github.libretube.extensions.normalize

class AudioHelper(private val context: Context) {
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val minimumVolumeIndex = AudioManagerCompat
        .getStreamMinVolume(audioManager, AudioManager.STREAM_MUSIC)
    private val maximumVolumeIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

   var videoVolume: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            BackgroundHelper.setVolume(context, field)
        }

    var systemVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - minimumVolumeIndex
        set(value) {
            val vol = value.coerceIn(minimumVolumeIndex, maximumVolumeIndex)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
        }

    fun setVolumeWithScale(value: Int, maxValue: Int, minValue: Int = 0) {
        if (PlayerHelper.swipeGestureSystemVolume) {
            systemVolume = value.normalize(minValue, maxValue, minimumVolumeIndex, maximumVolumeIndex)
        } else {
            videoVolume = value.toFloat().normalize(minValue.toFloat(), maxValue.toFloat(), 0f, 1f)
        }
    }

    fun getVolumeWithScale(maxValue: Int, minValue: Int = 0): Int {
        return if (PlayerHelper.swipeGestureSystemVolume) {
            systemVolume.normalize(minimumVolumeIndex, maximumVolumeIndex, minValue, maxValue)
        } else {
            videoVolume.normalize(0f, 1f, minValue.toFloat(), maxValue.toFloat()).toInt()
        }
    }
}
