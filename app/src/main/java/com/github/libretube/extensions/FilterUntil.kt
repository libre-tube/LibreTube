package com.github.libretube.extensions

/**
 * Returns a list of all items until the given condition is fulfilled
 * @param predicate The condition which needs to be searched for
 * @return a list of all items before the first true condition
 */
fun <T> List<T>.filterUntil(predicate: (T) -> Boolean): List<T>? {
    val items = mutableListOf<T>()
    this.forEach {
        if (predicate(it)) return items
        items.add(it)
    }
    return null
}
