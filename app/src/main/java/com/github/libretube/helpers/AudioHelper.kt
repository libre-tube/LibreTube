package com.github.libretube.helpers

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.common.audio.AudioManagerCompat
import androidx.media3.common.util.UnstableApi

@UnstableApi
class AudioHelper(context: Context) {
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val minimumVolumeIndex = AudioManagerCompat
        .getStreamMinVolume(audioManager, C.STREAM_TYPE_MUSIC)
    private val maximumVolumeIndex = AudioManagerCompat
        .getStreamMaxVolume(audioManager, C.STREAM_TYPE_MUSIC)
    private val volumeRange = maximumVolumeIndex - minimumVolumeIndex

    var deviceVolume: Float
        get() = run {
            val volume =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - minimumVolumeIndex
            volume.toFloat() / volumeRange
        }
        set(value) {
            val vol = (value * volumeRange + minimumVolumeIndex).toInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                vol.coerceIn(minimumVolumeIndex, maximumVolumeIndex),
                0
            )
        }
}
