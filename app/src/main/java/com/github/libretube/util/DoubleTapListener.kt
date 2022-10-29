package com.github.libretube.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View

abstract class DoubleTapListener : View.OnClickListener {

    private val handler = Handler(Looper.getMainLooper())

    private var lastClick = 0L
    private var lastDoubleClick = 0L

    abstract fun onDoubleClick()
    abstract fun onSingleClick()

    override fun onClick(v: View?) {
        if (isSecondClick()) {
            handler.removeCallbacks(runnable)
            lastDoubleClick = elapsedTime()
            onDoubleClick()
        } else {
            if (recentDoubleClick()) return
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, MAX_TIME_DIFF)
            lastClick = elapsedTime()
        }
    }

    private val runnable = Runnable {
        if (isSecondClick()) return@Runnable
        onSingleClick()
    }

    private fun isSecondClick(): Boolean {
        return elapsedTime() - lastClick < MAX_TIME_DIFF
    }

    private fun recentDoubleClick(): Boolean {
        return elapsedTime() - lastDoubleClick < MAX_TIME_DIFF
    }

    fun elapsedTime() = SystemClock.elapsedRealtime()

    companion object {
        private const val MAX_TIME_DIFF = 400L
    }
}
