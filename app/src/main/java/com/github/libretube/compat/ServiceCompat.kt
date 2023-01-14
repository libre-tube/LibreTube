package com.github.libretube.compat

import android.content.Context
import android.content.Intent
import android.os.Build

class ServiceCompat(private val context: Context) {
    fun startForeground(intent: Intent) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
