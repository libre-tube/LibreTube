package com.github.libretube.extensions

fun <T> awaitQuery(
    query: () -> T
):T {
    var x: T? = null
    val thread = Thread {
        x = query.invoke()
    }
    thread.start()
    thread.join()
    return x!!
}