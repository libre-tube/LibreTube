package com.github.libretube.ui.extensions

import android.text.format.DateUtils
import android.widget.TextView
import com.github.libretube.R

fun TextView.setFormattedDuration(duration: Long, isShort: Boolean?) {
    this.text = when {
        isShort == true -> context.getString(R.string.yt_shorts)
        duration < 0L -> context.getString(R.string.live)
        else -> DateUtils.formatElapsedTime(duration)
    }
}
