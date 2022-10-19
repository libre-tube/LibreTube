package com.github.libretube.extensions

import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.github.libretube.db.DatabaseHolder.Companion.Database

/**
 * shows the already watched time under the video
 */
fun View?.setWatchProgressLength(videoId: String, duration: Long) {
    val view = this!!

    val progress = try {
        awaitQuery {
            Database.watchPositionDao().findById(videoId)?.position
        }
    } catch (e: Exception) {
        return
    }

    view.viewTreeObserver
        .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@setWatchProgressLength.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (progress == null || duration == 0L) {
                    view.visibility = View.GONE
                    return
                }
                val fullWidth = (parent as LinearLayout).width
                val newWidth = (fullWidth * (progress / duration)) / 1000
                val lp = view.layoutParams
                lp.width = newWidth.toInt()
                view.layoutParams = lp
                view.visibility = View.VISIBLE
            }
        })
}
