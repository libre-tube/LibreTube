package com.github.libretube.helpers

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            val newUri = URI(
                originalUri.scheme.lowercase(Locale.US),
                URI(proxyUrl).authority,
                originalUri.path,
                originalUri.query,
                originalUri.fragment
            )
            return URLDecoder.decode(newUri.toString(), StandardCharsets.UTF_8.toString())
        }
        return url
    }
}
