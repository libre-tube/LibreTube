package com.github.libretube.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View

abstract class DoubleTapListener : View.OnClickListener {

    private var isSingleEvent = false
    private val doubleClickQualificationSpanInMillis: Long
    private var timestampLastClick: Long
    private val handler: Handler
    private val runnable: Runnable

    override fun onClick(v: View?) {
        if (SystemClock.elapsedRealtime() - timestampLastClick < doubleClickQualificationSpanInMillis) {
            isSingleEvent = false
            handler.removeCallbacks(runnable)
            onDoubleClick()
            return
        }
        isSingleEvent = true
        handler.postDelayed(runnable, DEFAULT_QUALIFICATION_SPAN)
        timestampLastClick = SystemClock.elapsedRealtime()
    }

    abstract fun onDoubleClick()
    abstract fun onSingleClick()

    companion object {
        private const val DEFAULT_QUALIFICATION_SPAN: Long = 200
    }

    init {
        doubleClickQualificationSpanInMillis = DEFAULT_QUALIFICATION_SPAN
        timestampLastClick = 0
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            if (isSingleEvent) {
                onSingleClick()
            }
        }
    }
}
