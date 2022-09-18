package com.github.libretube.extensions

/**
 * Replace file name specific chars
 */
fun String.sanitize(): String {
    return this.replace("[^a-zA-Z0-9\\._]+", "_")
}
