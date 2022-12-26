package com.github.libretube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.PlaylistBookmarkRowBinding
import com.github.libretube.databinding.PlaylistRowBinding

class PlaylistBookmarkViewHolder : RecyclerView.ViewHolder {
    var playlistBookmarkBinding: PlaylistBookmarkRowBinding? = null
    var playlistBinding: PlaylistRowBinding? = null

    constructor(binding: PlaylistBookmarkRowBinding) : super(binding.root) {
        playlistBookmarkBinding = binding
    }

    constructor(binding: PlaylistRowBinding) : super(binding.root) {
        playlistBinding = binding
    }
}
