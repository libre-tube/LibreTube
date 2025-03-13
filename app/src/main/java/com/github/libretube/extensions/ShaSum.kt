package com.github.libretube.extensions

import java.security.MessageDigest

/**
 * Calculates the SHA-256 hash of the String and returns the result in hexadecimal.
 */
@OptIn(ExperimentalStdlibApi::class)
fun String.sha256Sum(): String = MessageDigest.getInstance("SHA-256")
    .digest(this.toByteArray())
    .toHexString()
