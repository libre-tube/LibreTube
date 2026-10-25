package com.github.libretube.extensions

fun <T> MutableList<T>.move(oldPosition: Int, newPosition: Int) {
    if (oldPosition !in indices) return
    val item = this.removeAt(oldPosition)
    val target = newPosition.coerceIn(0, size)
    this.add(target, item)
}
