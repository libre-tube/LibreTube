package com.github.libretube.views

import android.os.Handler
import android.os.Looper
import android.view.View

class DoubleClickListener(
    private val doubleClickTimeLimitMills: Long = 200,
    private val callback: Callback
) : View.OnClickListener {
    private var lastClicked: Long = -1L

    override fun onClick(v: View?) {
        lastClicked = when {
            lastClicked == -1L -> {
                checkForSingleClick()
                System.currentTimeMillis()
            }
            isDoubleClicked() -> {
                callback.doubleClicked()
                -1L
            }
            else -> {
                checkForSingleClick()
                System.currentTimeMillis()
            }
        }
    }

    private fun checkForSingleClick() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (lastClicked != -1L) callback.singleClicked()
        }, doubleClickTimeLimitMills)
    }

    private fun getTimeDiff(from: Long, to: Long): Long {
        return to - from
    }

    private fun isDoubleClicked(): Boolean {
        return getTimeDiff(
            lastClicked,
            System.currentTimeMillis()
        ) <= doubleClickTimeLimitMills
    }

    interface Callback {
        fun doubleClicked()
        fun singleClicked()
    }
}
