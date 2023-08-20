package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel : ViewModel() {
    val isMiniPlayerVisible = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isFullscreen = MutableLiveData<Boolean>().apply {
        value = false
    }

    var maxSheetHeightPx = 0
}
