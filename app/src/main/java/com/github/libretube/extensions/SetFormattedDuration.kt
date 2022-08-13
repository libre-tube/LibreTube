package com.github.libretube.extensions

import android.text.format.DateUtils
import android.widget.TextView
import com.github.libretube.R

fun TextView?.setFormattedDuration(duration: Long) {
    val text = if (duration < 0L) {
        this!!.setBackgroundColor(R.attr.colorPrimaryDark)
        this.context.getString(R.string.live)
    } else if (duration == 0L) this!!.context.getString(R.string.yt_shorts)
    else DateUtils.formatElapsedTime(duration)
    this!!.text = text
}
