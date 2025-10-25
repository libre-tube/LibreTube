package com.github.libretube.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.DownloadPlaylistWithDownload
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadPlaylistViewHolder(val binding: PlaylistsRowBinding) :
    RecyclerView.ViewHolder(binding.root)

class DownloadPlaylistAdapter(
    val navigateToPlaylist: (playlist: DownloadPlaylistWithDownload) -> Unit
) :
    ListAdapter<DownloadPlaylistWithDownload, DownloadPlaylistViewHolder>(
        DiffUtilItemCallback<DownloadPlaylistWithDownload>(
            areItemsTheSame = { a, b -> a.downloadPlaylist.playlistId == b.downloadPlaylist.playlistId }
        )
    ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DownloadPlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlaylistsRowBinding.inflate(layoutInflater, parent, false)
        return DownloadPlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DownloadPlaylistViewHolder,
        position: Int
    ) {
        val item = getItem(position)!!

        with(holder.binding) {
            playlistTitle.text = item.downloadPlaylist.title
            playlistDescription.text = item.downloadPlaylist.description
            ImageHelper.loadImage(
                item.downloadPlaylist.thumbnailPath?.toUri()?.toString(),
                playlistThumbnail
            )
            videoCount.text = item.downloadVideos.size.toString()

            root.setOnClickListener {
                navigateToPlaylist(item)
            }
        }
    }

    /**
     * Delete a playlist from the database.
     *
     * If [includeVideos] is set to true, all corresponding download items will be deleted as well.
     */
    private fun deletePlaylist(position: Int, includeVideos: Boolean) {
        val playlist = getItem(position)!!

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                if (includeVideos) {
                    val downloads = DatabaseHolder.Database.downloadDao()
                        .getDownloadPlaylistByIdIncludingItems(playlist.downloadPlaylist.playlistId)
                        .downloadVideos

                    for (download in downloads) {
                        DownloadHelper.deleteDownloadIncludingFiles(download)
                    }
                }

                DatabaseHolder.Database.downloadDao().deletePlaylistIncludingVideoRefs(playlist.downloadPlaylist)
            }

            val updatedList = currentList.filter {
                it.downloadPlaylist.playlistId != playlist.downloadPlaylist.playlistId
            }
            submitList(updatedList)
        }
    }

    fun showDeleteDialog(context: Context, position: Int) {
        var includeVideos = false

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.deletePlaylist)
            .setMultiChoiceItems(
                arrayOf(context.getString(R.string.delete_playlist_include_vidoes)),
                null
            ) { _, _, checked ->
                includeVideos = checked
            }
            .setPositiveButton(R.string.okay) { _, _ ->
                deletePlaylist(position, includeVideos)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun restoreItem(position: Int) {
        // moves the item back to its initial horizontal position
        notifyItemRemoved(position)
        notifyItemInserted(position)
    }
}