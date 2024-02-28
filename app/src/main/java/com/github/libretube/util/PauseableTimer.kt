package com.github.libretube.util

import android.os.Handler
import android.os.Looper
import java.util.Timer
import java.util.TimerTask

class PauseableTimer(
    private val onTick: () -> Unit,
    private val delayMillis: Long = 1000L
) {
    val handler: Handler = Handler(Looper.getMainLooper())

    var timer: Timer? = null

    init {
        resume()
    }

    fun resume() {
        if (timer == null) timer = Timer()

        timer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    handler.post(onTick)
                }
            },
            delayMillis,
            delayMillis
        )
    }

    fun pause() {
        timer?.cancel()
        timer = null
    }

    fun destroy() {
        timer?.cancel()
        timer = null
    }
}
