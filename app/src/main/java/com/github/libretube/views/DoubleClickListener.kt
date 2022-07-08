package com.github.libretube.views

import android.os.Handler
import android.os.Looper
import android.view.View

class DoubleClickListener(
    private val doubleClickTimeLimitMills: Long = 300,
    private val callback: Callback
) : View.OnClickListener {
    private var lastClicked: Long = -1L
    private var doubleClicked: Boolean = false

    override fun onClick(v: View?) {
        lastClicked = when {
            lastClicked == -1L -> {
                doubleClicked = false
                System.currentTimeMillis()
            }
            isDoubleClicked() -> {
                doubleClicked = true
                callback.doubleClicked()
                -1L
            }
            else -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!doubleClicked) callback.singleClicked()
                }, doubleClickTimeLimitMills)
                System.currentTimeMillis()
            }
        }
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
