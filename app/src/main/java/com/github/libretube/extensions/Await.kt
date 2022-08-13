package com.github.libretube.extensions

fun Thread.await() {
    this.apply {
        start()
        join()
    }
}
