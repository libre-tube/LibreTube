package com.github.libretube.util

import java.util.Timer
import java.util.TimerTask

abstract class CustomTimer(private var delay: Long, private var period: Long) {
    private lateinit var timer: Timer

    fun start() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                onTimerTick()
            }
        }, delay, period)
    }

    fun updateState(state: Boolean) {
        if (state) start()
        else timer.cancel()
    }

    abstract fun onTimerTick()
}