package com.github.libretube.util

import com.github.libretube.helpers.PreferenceHelper

class ExceptionHandler(
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        // OkHttp spawns threads to parse the response headers
        // if an exception is on a thread spawned by OkHttp, there's no apparent other way to catch them
        // work around for Cronet spawning different threads with uncaught Exception when used with Coil
        // for example this catches crashes when there are invalid values in the header
        if (thread.name == OKHTTP_THREAD_NAME) return

        // save the error log
        PreferenceHelper.saveErrorLog(exc.stackTraceToString())
        // throw the exception with the default exception handler to make the app crash
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }

    companion object {
        private const val OKHTTP_THREAD_NAME = "OkHttp Dispatcher"
    }
}
