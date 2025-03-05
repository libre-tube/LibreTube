package com.github.libretube.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.CustomInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.MalformedURLException

class CustomInstancesModel: ViewModel() {
    val instances = Database.customInstanceDao().getAllFlow()
        .flowOn(Dispatchers.IO)

    fun addCustomInstance(apiUrlInput: String, instanceNameInput: String?, frontendUrlInput: String?) {
        if (apiUrlInput.isEmpty()) throw IllegalArgumentException()

        val apiUrl = apiUrlInput.toHttpUrlOrNull() ?: throw MalformedURLException()
        val frontendUrl = if (!frontendUrlInput.isNullOrBlank()) {
            frontendUrlInput.toHttpUrlOrNull() ?: throw MalformedURLException()
        } else {
            null
        }

        viewModelScope.launch(Dispatchers.IO) {
            val instanceName = instanceNameInput ?: apiUrl.host

            Database.customInstanceDao()
                .insert(CustomInstance(instanceName, apiUrl.toString(), frontendUrl?.toString().orEmpty()))
        }
    }

    fun deleteCustomInstance(customInstance: CustomInstance) = viewModelScope.launch(Dispatchers.IO) {
        Database.customInstanceDao().deleteCustomInstance(customInstance)
    }
}