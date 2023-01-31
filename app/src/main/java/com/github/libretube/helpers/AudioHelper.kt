package com.github.libretube.helpers

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.core.math.MathUtils
import com.github.libretube.extensions.normalize

class AudioHelper(
    context: Context,
    private val stream: Int = AudioManager.STREAM_MUSIC
) {

    private lateinit var audioManager: AudioManager
    private var minimumVolumeIndex = 0
    private var maximumVolumeIndex = 16

    init {
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let {
            audioManager = it
            maximumVolumeIndex = it.getStreamMaxVolume(stream)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                minimumVolumeIndex = it.getStreamMinVolume(stream)
            }
        }
    }

    var volume: Int
        get() {
            return if (this::audioManager.isInitialized) {
                audioManager.getStreamVolume(stream) - minimumVolumeIndex
            } else {
                0
            }
        }
        set(value) {
            if (this::audioManager.isInitialized) {
                val vol = MathUtils.clamp(value, minimumVolumeIndex, maximumVolumeIndex)
                audioManager.setStreamVolume(stream, vol, 0)
            }
        }

    fun setVolumeWithScale(value: Int, maxValue: Int, minValue: Int = 0) {
        volume = value.normalize(minValue, maxValue, minimumVolumeIndex, maximumVolumeIndex)
    }

    fun getVolumeWithScale(maxValue: Int, minValue: Int = 0): Int {
        return volume.normalize(minimumVolumeIndex, maximumVolumeIndex, minValue, maxValue)
    }
}
