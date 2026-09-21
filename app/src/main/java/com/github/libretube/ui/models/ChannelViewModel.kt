package com.github.libretube.ui.models

import androidx.lifecycle.ViewModel
import com.github.libretube.api.obj.StreamItem

class ChannelViewModel : ViewModel() {
    // These are used for the first tab ("Videos") if they were pre-fetched with the channel
    var relatedStreams: List<StreamItem>? = null
    var nextPage: String? = null
}
