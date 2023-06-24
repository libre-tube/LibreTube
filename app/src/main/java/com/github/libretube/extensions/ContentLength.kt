package com.github.libretube.extensions

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun URL.getContentLength(): Long? {
    try {
        return withContext(Dispatchers.IO) {
            val connection = openConnection() as HttpURLConnection
            connection.setRequestProperty("Range", "bytes=0-")

            val value = connection.getHeaderField("content-length")
                // If connection accepts range header, try to get total bytes
                ?: connection.getHeaderField("content-range").split("/")[1]

            connection.disconnect()
            value.toLong()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}
