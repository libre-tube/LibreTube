package com.github.libretube.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View

abstract class DoubleTapListener : View.OnClickListener {

    private val maximumTimeDifference = 300L
    private val handler = Handler(Looper.getMainLooper())

    private var isSingleEvent = false
    private var timeStampLastClick = 0L
    private var timeStampLastDoubleClick = 0L

    override fun onClick(v: View?) {
        if (SystemClock.elapsedRealtime() - timeStampLastClick < maximumTimeDifference) {
            isSingleEvent = false
            handler.removeCallbacks(runnable)
            timeStampLastDoubleClick = SystemClock.elapsedRealtime()
            onDoubleClick()
            return
        }
        isSingleEvent = true
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, maximumTimeDifference)
        timeStampLastClick = SystemClock.elapsedRealtime()
    }

    abstract fun onDoubleClick()
    abstract fun onSingleClick()

    private val runnable = Runnable {
        if (!isSingleEvent ||
            SystemClock.elapsedRealtime() - timeStampLastDoubleClick < maximumTimeDifference
        ) return@Runnable
        onSingleClick()
    }
}
