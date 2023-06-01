package com.github.libretube.helpers

import android.content.Context
import android.content.ContextWrapper
import com.github.libretube.ui.activities.MainActivity

object ContextHelper {
    fun unwrapActivity(context: Context): MainActivity {
        var correctContext: Context? = context
        while (correctContext !is MainActivity && correctContext is ContextWrapper) {
            correctContext = correctContext.baseContext
        }
        return correctContext as MainActivity
    }
}
