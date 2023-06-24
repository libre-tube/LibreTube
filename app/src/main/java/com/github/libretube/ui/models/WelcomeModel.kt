package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.InstanceHelper
import com.github.libretube.api.obj.Instances
import com.github.libretube.extensions.toastFromMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WelcomeModel : ViewModel() {
    val selectedInstanceIndex = MutableLiveData<Int>()

    var instances = MutableLiveData<List<Instances>>()

    fun fetchInstances(context: Context) {
        if (!instances.value.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val instances = try {
                InstanceHelper.getInstances(context)
            } catch (e: Exception) {
                context.applicationContext.toastFromMainDispatcher(e.message.orEmpty())
                InstanceHelper.getInstancesFallback(context)
            }
            this@WelcomeModel.instances.postValue(instances)
        }
    }
}
