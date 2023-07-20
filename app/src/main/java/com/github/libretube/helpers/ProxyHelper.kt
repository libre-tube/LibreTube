package com.github.libretube.helpers

import com.github.libretube.api.CronetHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val proxyUrl = PreferenceHelper.getString(PreferenceKeys.IMAGE_PROXY_URL, "")
            .toHttpUrlOrNull() ?: return url

        return url?.toHttpUrlOrNull()?.newBuilder()
            ?.host(proxyUrl.host)
            ?.port(proxyUrl.port)
            ?.toString()
    }

    /**
     * Detect whether the proxy should be used or not for a given stream URL based on user preferences
     */
    suspend fun unwrapStreamUrl(url: String): String {
        return if (useYouTubeSourceWithoutProxy(url)) unwrapUrl(url) else url
    }

    suspend fun useYouTubeSourceWithoutProxy(url: String) = when {
        !PreferenceHelper.getBoolean(PreferenceKeys.DISABLE_VIDEO_IMAGE_PROXY, false) -> false
        PreferenceHelper.getBoolean(PreferenceKeys.FALLBACK_PIPED_PROXY, true) -> {
            // check whether the URL has content available, and disable proxy if that's the case
            isUrlUsable(unwrapUrl(url))
        }
        else -> true
    }

    fun unwrapImageUrl(url: String) = if (
        !PreferenceHelper.getBoolean(PreferenceKeys.DISABLE_VIDEO_IMAGE_PROXY, false)
    ) {
        url
    } else {
        unwrapUrl(url)
    }

    /**
     * Convert a proxied Piped url to a YouTube url that's not proxied
     */
    fun unwrapUrl(url: String, unwrap: Boolean = true) = url.toHttpUrlOrNull()
        ?.takeIf { unwrap }?.let {
            val host = it.queryParameter("host")
            // if there's no host parameter specified, there's no way to unwrap the URL
            // and the proxied one must be used. That's the case if using LBRY.
            if (host.isNullOrEmpty()) return@let url
            it.newBuilder()
                .host(host)
                .removeAllQueryParameters("host")
                .build()
                .toString()
        } ?: url

    /**
     * Parse the Piped url to a YouTube url (or not) based on preferences
     */
    private suspend fun isUrlUsable(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val connection = CronetHelper.cronetEngine.openConnection(URL(url)) as HttpURLConnection
            connection.requestMethod = "HEAD"
            val isSuccess = connection.responseCode == 200
            connection.disconnect()
            return@runCatching isSuccess
        }.getOrDefault(false)
    }
}
