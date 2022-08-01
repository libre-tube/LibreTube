package com.github.libretube.util

import android.content.Context
import android.content.Intent
import com.github.libretube.services.BackgroundMode

object BackgroundHelper {
    fun playOnBackground(
        context: Context,
        videoId: String,
        position: Long? = null
    ) {
        val intent = Intent(context, BackgroundMode::class.java)
        intent.putExtra("videoId", videoId)
        if (position != null) intent.putExtra("position", position)
        context.startForegroundService(intent)
    }
}
