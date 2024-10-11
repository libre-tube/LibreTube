package com.github.libretube.helpers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

object ContextHelper {
    inline fun <reified T : Activity> tryUnwrapActivity(context: Context): T? {
        var correctContext: Context? = context
        while (correctContext !is T && correctContext is ContextWrapper) {
            correctContext = correctContext.baseContext
        }
        return correctContext as? T
    }

    inline fun <reified T : Activity> unwrapActivity(context: Context): T = tryUnwrapActivity(context)!!
}
