package com.github.libretube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ChannelRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.databinding.VideoRowBinding

class SearchViewHolder : RecyclerView.ViewHolder {
    var videoRowBinding: VideoRowBinding? = null
    var channelRowBinding: ChannelRowBinding? = null
    var playlistRowBinding: PlaylistsRowBinding? = null

    constructor(binding: VideoRowBinding) : super(binding.root) {
        videoRowBinding = binding
    }

    constructor(binding: ChannelRowBinding) : super(binding.root) {
        channelRowBinding = binding
    }

    constructor(binding: PlaylistsRowBinding) : super(binding.root) {
        playlistRowBinding = binding
    }
}
