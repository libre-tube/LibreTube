package com.github.libretube.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.CustomInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException

class InstancesModel : ViewModel() {
    val customInstances = Database.customInstanceDao().getAllFlow()
        .flowOn(Dispatchers.IO)

    fun addCustomInstance(
        apiUrlInput: String,
        instanceNameInput: String?,
        frontendUrlInput: String?
    ) {
        if (apiUrlInput.isEmpty()) throw IllegalArgumentException()

        val apiUrl = apiUrlInput.toHttpUrlOrNull()
            ?.takeIf { isAllowedInstanceScheme(it) }
            ?: throw MalformedURLException()
        val frontendUrl = if (!frontendUrlInput.isNullOrBlank()) {
            frontendUrlInput.toHttpUrlOrNull()
                ?.takeIf { isAllowedInstanceScheme(it) }
                ?: throw MalformedURLException()
        } else {
            null
        }

        viewModelScope.launch(Dispatchers.IO) {
            val instanceName = instanceNameInput ?: apiUrl.host

            Database.customInstanceDao()
                .insert(
                    CustomInstance(
                        instanceName,
                        apiUrl.toString(),
                        frontendUrl?.toString().orEmpty()
                    )
                )
        }
    }

    fun deleteCustomInstance(customInstance: CustomInstance) =
        viewModelScope.launch(Dispatchers.IO) {
            Database.customInstanceDao().deleteCustomInstance(customInstance)
        }

    /**
     * Only HTTPS instances are accepted for new entries to avoid sending
     * Piped credentials/tokens over cleartext. Loopback addresses are still
     * allowed over plain HTTP so that locally self-hosted instances (e.g. a
     * dev server or a Home Assistant Piped setup) remain usable.
     */
    private fun isAllowedInstanceScheme(httpUrl: HttpUrl): Boolean {
        if (httpUrl.scheme == "https") return true
        return httpUrl.scheme == "http" && isLoopback(httpUrl.host)
    }

    private fun isLoopback(host: String): Boolean {
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host == "::1" ||
            host.startsWith("127.")
    }
}