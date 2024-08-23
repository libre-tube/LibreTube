package com.github.libretube.helpers

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ProxyHelper {
    fun fetchProxyUrl() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                RetrofitInstance.api.getConfig().imageProxyUrl?.let {
                    PreferenceHelper.putString(PreferenceKeys.IMAGE_PROXY_URL, it)
                }
            }
        }
    }

    fun rewriteUrl(url: String?): String? {
        if (url == null) return null

        val proxyUrl = PreferenceHelper.getString(PreferenceKeys.IMAGE_PROXY_URL, "")
            .toHttpUrlOrNull() ?: return url

        val parsedUrl = url.toHttpUrlOrNull() ?: return url
        if (parsedUrl.queryParameter("host").isNullOrEmpty()) {
            return parsedUrl.newBuilder()
                .host(proxyUrl.host)
                .port(proxyUrl.port)
                .setQueryParameter("host", parsedUrl.host)
                .build()
                .toString()
        }

        return parsedUrl.newBuilder()
            .host(proxyUrl.host)
            .port(proxyUrl.port)
            .build()
            .toString()
    }

    /**
     * Detect whether the proxy should be used or not for a given stream URL based on user preferences
     */
    fun unwrapStreamUrl(url: String): String {
        return if (PlayerHelper.disablePipedProxy && !PlayerHelper.localStreamExtraction) {
            unwrapUrl(url)
        } else {
            url
        }
    }

    fun unwrapImageUrl(url: String): String {
        return if (PlayerHelper.disablePipedProxy) {
            unwrapUrl(url)
        } else {
            url
        }
    }

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied
     */
    fun unwrapUrl(url: String, unwrap: Boolean = true): String {
        val parsedUrl = url.toHttpUrlOrNull()

        val host = parsedUrl?.queryParameter("host")
        // if there's no host parameter specified, there's no way to unwrap the URL
        // and the proxied one must be used. That's the case if using LBRY.
        if (!unwrap || parsedUrl == null || host.isNullOrEmpty()) {
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
