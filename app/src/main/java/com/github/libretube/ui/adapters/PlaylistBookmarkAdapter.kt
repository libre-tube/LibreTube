package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.PlaylistBookmarkRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.query
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.viewholders.PlaylistBookmarkViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class PlaylistBookmarkAdapter(
    private val bookmarks: List<PlaylistBookmark>,
    private val bookmarkMode: BookmarkMode = BookmarkMode.FRAGMENT
) : RecyclerView.Adapter<PlaylistBookmarkViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistBookmarkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (bookmarkMode) {
            BookmarkMode.HOME -> PlaylistBookmarkViewHolder(
                PlaylistBookmarkRowBinding.inflate(layoutInflater, parent, false)
            )
            BookmarkMode.FRAGMENT -> PlaylistBookmarkViewHolder(
                PlaylistsRowBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    override fun onBindViewHolder(holder: PlaylistBookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.playlistBookmarkBinding?.apply {
            ImageHelper.loadImage(bookmark.thumbnailUrl, thumbnail)
            playlistName.text = bookmark.playlistName
            uploaderName.text = bookmark.uploader

            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(
                    root.context,
                    bookmark.playlistId,
                    PlaylistType.PUBLIC
                )
            }

            root.setOnLongClickListener {
                PlaylistOptionsBottomSheet(
                    playlistId = bookmark.playlistId,
                    playlistName = bookmark.playlistName ?: "",
                    playlistType = PlaylistType.PUBLIC
                ).show(
                    (root.context as AppCompatActivity).supportFragmentManager
                )
                true
            }
        }

        holder.playlistsBinding?.apply {
            // hide the count of videos inside the playlist as it's not stored in the database
            videoCount.visibility = View.GONE

            var isBookmarked = true

            ImageHelper.loadImage(bookmark.thumbnailUrl, playlistThumbnail)
            playlistTitle.text = bookmark.playlistName
            playlistDescription.text = bookmark.uploader

            deletePlaylist.setImageResource(R.drawable.ic_bookmark)
            deletePlaylist.setOnClickListener {
                isBookmarked = !isBookmarked
                deletePlaylist.setImageResource(
                    if (isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outlined
                )
                query {
                    if (!isBookmarked) {
                        DatabaseHolder.Database.playlistBookmarkDao()
                            .deleteById(bookmark.playlistId)
                    } else {
                        DatabaseHolder.Database.playlistBookmarkDao()
                            .insertAll(bookmark)
                    }
                }
            }

            deletePlaylist.visibility = View.VISIBLE

            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(
                    root.context,
                    bookmark.playlistId,
                    PlaylistType.PUBLIC
                )
            }

            root.setOnLongClickListener {
                PlaylistOptionsBottomSheet(
                    playlistId = bookmark.playlistId,
                    playlistName = bookmark.playlistName ?: "",
                    playlistType = PlaylistType.PUBLIC
                ).show(
                    (root.context as AppCompatActivity).supportFragmentManager
                )
                true
            }
        }
    }

    companion object {
        enum class BookmarkMode {
            HOME,
            FRAGMENT
        }
    }
}
