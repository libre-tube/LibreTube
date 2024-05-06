package com.github.libretube.util

import android.os.Looper
import com.github.libretube.helpers.PreferenceHelper

class ExceptionHandler(
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, exc: Throwable) {
        // if the exception is not on the main thread, the app is still functional and responsive
        // hence the app doesn't have to be quit fully
        // work around for Cronet spawning different threads with uncaught Exception when used with Coil
        if (thread.id != Looper.getMainLooper().thread.id) return

        // save the error log
        PreferenceHelper.saveErrorLog(exc.stackTraceToString())
        // throw the exception with the default exception handler to make the app crash
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
