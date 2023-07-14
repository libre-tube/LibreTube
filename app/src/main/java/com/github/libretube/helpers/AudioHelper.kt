package com.github.libretube.helpers

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.media.AudioManagerCompat
import com.github.libretube.extensions.normalize

class AudioHelper(context: Context) {
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val minimumVolumeIndex = AudioManagerCompat
        .getStreamMinVolume(audioManager, AudioManager.STREAM_MUSIC)
    private val maximumVolumeIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var volume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - minimumVolumeIndex
        set(value) {
            val vol = value.coerceIn(minimumVolumeIndex, maximumVolumeIndex)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
        }

    fun setVolumeWithScale(value: Int, maxValue: Int, minValue: Int = 0) {
        volume = value.normalize(minValue, maxValue, minimumVolumeIndex, maximumVolumeIndex)
    }

    fun getVolumeWithScale(maxValue: Int, minValue: Int = 0): Int {
        return volume.normalize(minimumVolumeIndex, maximumVolumeIndex, minValue, maxValue)
    }
}
