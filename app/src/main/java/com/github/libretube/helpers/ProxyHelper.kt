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
        val proxyUrl = PreferenceHelper.getString(PreferenceKeys.IMAGE_PROXY_URL, "")
            .toHttpUrlOrNull() ?: return url

        return url?.toHttpUrlOrNull()?.newBuilder()
            ?.host(proxyUrl.host)
            ?.port(proxyUrl.port)
            ?.toString()
    }

    /**
     * Load the YT url directly instead of the proxied Piped URL if enabled
     */
    fun unwrapIfEnabled(url: String): String {
        if (!PreferenceHelper.getBoolean(
                PreferenceKeys.DISABLE_VIDEO_IMAGE_PROXY,
                false
            )
        ) {
            return url
        }

        return url.toHttpUrlOrNull()?.let {
            it.newBuilder()
                .host(it.queryParameter("host").orEmpty())
                .removeAllQueryParameters("host")
                .build()
        }.toString()
    }
}
