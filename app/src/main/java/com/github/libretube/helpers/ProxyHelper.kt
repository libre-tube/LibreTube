package com.github.libretube.helpers

import com.github.libretube.api.PipedMediaServiceRepository
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
                RetrofitInstance.externalApi.getInstanceConfig(PipedMediaServiceRepository.apiUrl)
                    .imageProxyUrl?.let {
                        PreferenceHelper.putString(PreferenceKeys.IMAGE_PROXY_URL, it)
                    }
            }
        }
    }

    /**
     * Decide whether the proxy should be used or not for a given stream URL based on user preferences
     */
    fun rewriteUrlUsingProxyPreference(url: String): String {
        if (PlayerHelper.disablePipedProxy) {
            return unwrapUrl(url)
        }

        return proxyRewriteUrl(url) ?: url
    }

    /**
     * Rewrite the URL to use the stored image proxy url of the selected instance.
     * Can handle both Piped links and normal YouTube links.
     */
    private fun proxyRewriteUrl(url: String?): String? {
        if (url == null) return null

        val proxyUrl = PreferenceHelper.getString(PreferenceKeys.IMAGE_PROXY_URL, "")
            .toHttpUrlOrNull()

        // parsedUrl should now be a plain YouTube URL without using any proxy
        val parsedUrl = unwrapUrl(url).toHttpUrlOrNull()
        if (proxyUrl == null || parsedUrl == null) return null

        return parsedUrl.newBuilder()
            .host(proxyUrl.host)
            .port(proxyUrl.port)
            .setQueryParameter("host", parsedUrl.host)
            .build()
            .toString()
    }

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied
     *
     * Should not be called directly in most cases, use [rewriteUrlUsingProxyPreference] instead
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
