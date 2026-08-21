package com.github.libretube.extensions

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun runCatchingIO(tag: String = "runCatchingIO", block: suspend () -> Unit) = scope.launch {
    runCatching { block() }
        .onFailure { Log.e(tag, it.message.orEmpty(), it) }
}
