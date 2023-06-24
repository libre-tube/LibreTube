package com.github.libretube.extensions

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Context.toastFromMainThread(text: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun Context.toastFromMainThread(stringId: Int) {
    toastFromMainThread(getString(stringId))
}

suspend fun Context.toastFromMainDispatcher(text: String, length: Int = Toast.LENGTH_SHORT) {
    withContext(Dispatchers.Main) {
        Toast.makeText(this@toastFromMainDispatcher, text, length).show()
    }
}

suspend fun Context.toastFromMainDispatcher(stringId: Int, length: Int = Toast.LENGTH_SHORT) {
    toastFromMainDispatcher(getString(stringId), length)
}
