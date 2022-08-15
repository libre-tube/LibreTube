package com.github.libretube.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel : ViewModel() {
    val isMiniPlayerVisible = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isFullscreen = MutableLiveData<Boolean>().apply {
        value = false
    }
}
