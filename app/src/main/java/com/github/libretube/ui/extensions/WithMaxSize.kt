package com.github.libretube.ui.extensions

fun <T> List<T>.withMaxSize(maxSize: Int): List<T> {
    return this.filterIndexed { index, _ -> index < maxSize }
}
