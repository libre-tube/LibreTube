package com.github.libretube.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> List<A>.parallelMap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async(Dispatchers.IO) { f(it) } }.awaitAll()
}
