package com.github.libretube.util

class ExceptionHandler(
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, exc: Throwable) {
        // save the error log
        PreferenceHelper.saveErrorLog(exc.stackTraceToString())
        // throw the exception with the default exception handler to make the app crash
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
