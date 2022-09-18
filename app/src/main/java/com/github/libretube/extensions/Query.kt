package com.github.libretube.extensions

fun query(block: () -> Unit) {
    Thread(block).start()
}
