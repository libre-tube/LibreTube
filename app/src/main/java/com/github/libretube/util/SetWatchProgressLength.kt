package com.github.libretube.util

import android.view.View
import android.view.ViewTreeObserver
import com.github.libretube.preferences.PreferenceHelper

/**
 * shows the already watched time under the video
 */
fun View?.setWatchProgressLength(videoId: String, duration: Long) {
    val view = this!!
    val positions = PreferenceHelper.getWatchPositions()
    var newWidth: Long? = null
    view.getViewTreeObserver()
        .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@setWatchProgressLength.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                positions.forEach {
                    if (it.videoId == videoId) {
                        newWidth = (width * (it.position / (duration))) / 1000
                        return@forEach
                    }
                }
                if (newWidth != null) {
                    val lp = view.layoutParams
                    lp.apply {
                        width = newWidth!!.toInt()
                    }
                    view.layoutParams = lp
                } else {
                    view.visibility = View.GONE
                }
            }
        })
}
