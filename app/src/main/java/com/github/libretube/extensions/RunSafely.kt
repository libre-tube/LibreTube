package com.github.libretube.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> runSafely(
    onSuccess: (List<T>) -> Unit = { },
    ioBlock: suspend () -> List<T>
) {
    val result = withContext(Dispatchers.IO) {
        runCatching { ioBlock() }.getOrNull()
    }
    if (!result.isNullOrEmpty()) {
        withContext(Dispatchers.Main) { onSuccess(result) }
    }
}
