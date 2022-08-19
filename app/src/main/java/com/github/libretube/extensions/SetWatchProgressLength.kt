package com.github.libretube.util

import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.await

/**
 * shows the already watched time under the video
 */
fun View?.setWatchProgressLength(videoId: String, duration: Long) {
    val view = this!!
    var progress: Long? = null

    Thread {
        try {
            progress = DatabaseHolder.db.watchPositionDao().findById(videoId).position
        } catch (e: Exception) {
            progress = null
        }
    }.await()

    view.getViewTreeObserver()
        .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@setWatchProgressLength.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                if (progress == null || duration == 0L) {
                    view.visibility = View.GONE
                    return
                }
                val fullWidth = (parent as LinearLayout).width
                val newWidth = (fullWidth * (progress!! / (duration))) / 1000
                val lp = view.layoutParams
                lp.width = newWidth.toInt()
                view.layoutParams = lp
                view.visibility = View.VISIBLE
            }
        })
}
