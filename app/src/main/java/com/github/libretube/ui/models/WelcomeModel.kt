package com.github.libretube.ui.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.InstanceHelper
import com.github.libretube.api.obj.Instances
import com.github.libretube.extensions.toastFromMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeModel(private val application: Application) : AndroidViewModel(application) {
    val selectedInstanceIndex = MutableLiveData<Int>()

    var instances = MutableLiveData<List<Instances>>()

    fun fetchInstances() {
        if (!instances.value.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val instances = try {
                InstanceHelper.getInstances(application)
            } catch (e: Exception) {
                application.toastFromMainDispatcher(e.message.orEmpty())
                InstanceHelper.getInstancesFallback(application)
            }
            this@WelcomeModel.instances.postValue(instances)
        }
    }
}
