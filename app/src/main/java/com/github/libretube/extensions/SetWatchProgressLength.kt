package com.github.libretube.util

import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
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
                        val fullWidth = (parent as LinearLayout).width
                        newWidth = (fullWidth * (it.position / (duration))) / 1000
                        return@forEach
                    }
                }
                if (newWidth != null) {
                    val lp = view.layoutParams
                    lp.apply {
                        width = newWidth!!.toInt()
                    }
                    view.layoutParams = lp
                    view.visibility = View.VISIBLE
                } else {
                    view.visibility = View.GONE
                }
            }
        })
}
