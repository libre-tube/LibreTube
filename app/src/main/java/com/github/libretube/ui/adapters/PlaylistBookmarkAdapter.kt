package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.PlaylistBookmarkRowBinding
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.viewholders.PlaylistBookmarkViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class PlaylistBookmarkAdapter(
    private val bookmarks: List<PlaylistBookmark>
) : RecyclerView.Adapter<PlaylistBookmarkViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistBookmarkViewHolder {
        val binding = PlaylistBookmarkRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistBookmarkViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    override fun onBindViewHolder(holder: PlaylistBookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.binding.apply {
            ImageHelper.loadImage(bookmark.thumbnailUrl, thumbnail)
            playlistName.text = bookmark.playlistName
            uploaderName.text = bookmark.uploader

            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, bookmark.playlistId, false)
            }

            root.setOnLongClickListener {
                PlaylistOptionsBottomSheet(
                    playlistId = bookmark.playlistId,
                    playlistName = bookmark.playlistName ?: "",
                    isOwner = false
                ).show(
                    (root.context as AppCompatActivity).supportFragmentManager
                )
                true
            }
        }
    }
}
