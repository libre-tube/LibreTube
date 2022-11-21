package com.github.libretube.util

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

object ProxyHelper {
    private fun getImageProxyUrl(): String? {
        val url = PreferenceHelper.getString(PreferenceKeys.IMAGE_PROXY_URL, "")
        return if (url != "") url else null
    }

    private fun setImageProxyUrl(url: String) {
        PreferenceHelper.putString(PreferenceKeys.IMAGE_PROXY_URL, url)
    }

    fun fetchProxyUrl() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                RetrofitInstance.api.getConfig().imageProxyUrl?.let {
                    setImageProxyUrl(it)
                }
            }
        }
    }

    fun rewriteUrl(url: String?): String? {
        url ?: return null

        val proxyUrl = getImageProxyUrl()
        proxyUrl ?: return url

        runCatching {
            val originalUri = URI(url)
            return URI(
                originalUri.scheme.lowercase(),
                proxyUrl,
                originalUri.path,
                originalUri.query,
                originalUri.fragment
            ).toString()
        }
        return url
    }
}
