package com.github.libretube.util

import android.util.Log
import com.github.libretube.preferences.PreferenceHelper
import kotlin.system.exitProcess

class ExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Log.e("bnyro", exc.stackTraceToString())
        // sav ethe error log
        PreferenceHelper.saveErrorLog(exc.stackTraceToString())
        // finish the app
        System.exit(0)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }
}
