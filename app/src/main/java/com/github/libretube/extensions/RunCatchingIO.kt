package com.github.libretube.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun runCatchingIO(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            block.invoke()
        }
    }
}
