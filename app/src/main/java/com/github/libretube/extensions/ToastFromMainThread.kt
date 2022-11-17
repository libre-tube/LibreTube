package com.github.libretube.extensions

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

fun Context.toastFromMainThread(stringId: Int) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            this,
            stringId,
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun Context.toastFromMainThread(text: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}
