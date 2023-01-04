package com.github.libretube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.PlaylistBookmarkRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding

class PlaylistBookmarkViewHolder : RecyclerView.ViewHolder {
    var playlistBookmarkBinding: PlaylistBookmarkRowBinding? = null
    var playlistsBinding: PlaylistsRowBinding? = null

    constructor(binding: PlaylistBookmarkRowBinding) : super(binding.root) {
        playlistBookmarkBinding = binding
    }

    constructor(binding: PlaylistsRowBinding) : super(binding.root) {
        playlistsBinding = binding
    }
}
