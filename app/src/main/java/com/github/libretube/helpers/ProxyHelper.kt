package com.github.libretube.helpers

import com.github.libretube.constants.PreferenceKeys
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ProxyHelper {

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied
     */
    fun unwrapUrl(url: String): String {
        val parsedUrl = url.toHttpUrlOrNull() ?: return url

        val host = parsedUrl.queryParameter("host")
        // If the host is not set, the URL is probably already unwrapped
        if (host.isNullOrEmpty()) {
            return url
        }

        return parsedUrl.newBuilder()
            .host(host)
            .removeAllQueryParameters("host")
            .removeAllQueryParameters("qhash")
            .build()
            .toString()
    }
}
