package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.github.libretube.api.obj.ChapterSegment

class PlayerViewModel : ViewModel() {
    var player: ExoPlayer? = null

    val isMiniPlayerVisible = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isFullscreen = MutableLiveData<Boolean>().apply {
        value = false
    }

    var maxSheetHeightPx = 0

    val chaptersLiveData = MutableLiveData<List<ChapterSegment>>()

    val chapters get() = chaptersLiveData.value.orEmpty()
}
