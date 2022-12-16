package com.github.libretube.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun URL.getContentLength(def: Long = -1): Long {
    try {
        return withContext(Dispatchers.IO) {
            val con = openConnection() as HttpURLConnection
            con.setRequestProperty("Range", "bytes=0-")

            val value = con.getHeaderField("content-length")
                // If connection accepts range header, try to get total bytes
                ?: con.getHeaderField("content-range").split("/")[1]

            value.toLong()
        }
    } catch (e: Exception) { e.printStackTrace() }

    return def
}
