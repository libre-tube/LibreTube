package com.github.libretube.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> runSafely(
    onSuccess: (List<T>) -> Unit = { },
    ioBlock: suspend () -> List<T>
) {
    withContext(Dispatchers.IO) {
        val result = runCatching { ioBlock.invoke() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: return@withContext

        withContext(Dispatchers.Main) {
            if (result.isNotEmpty()) {
                onSuccess.invoke(result)
            }
        }
    }
}
