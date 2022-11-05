package com.github.libretube.ui.adapters

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.api.obj.Playlists
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.viewholders.PlaylistsViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PlaylistsAdapter(
    private val playlists: MutableList<Playlists>,
    private val childFragmentManager: FragmentManager
) : RecyclerView.Adapter<PlaylistsViewHolder>() {

    override fun getItemCount(): Int {
        return playlists.size
    }

    fun updateItems(newItems: List<Playlists>) {
        val oldSize = playlists.size
        playlists.addAll(newItems)
        notifyItemRangeInserted(oldSize, playlists.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlaylistsRowBinding.inflate(layoutInflater, parent, false)
        return PlaylistsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistsViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.binding.apply {
            // set imageview drawable as empty playlist if imageview empty
            if (playlist.thumbnail!!.split("/").size <= 4) {
                playlistThumbnail.setImageResource(R.drawable.ic_empty_playlist)
                playlistThumbnail.setBackgroundColor(R.attr.colorSurface)
            } else {
                ImageHelper.loadImage(playlist.thumbnail, playlistThumbnail)
            }
            playlistTitle.text = playlist.name

            videoCount.text = playlist.videos.toString()

            deletePlaylist.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(root.context)
                builder.setTitle(R.string.deletePlaylist)
                builder.setMessage(R.string.areYouSure)
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    PreferenceHelper.getToken()
                    deletePlaylist(root.context as Activity, playlist.id!!, position)
                }
                builder.setNegativeButton(R.string.cancel, null)
                builder.show()
            }
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, playlist.id, true)
            }

            root.setOnLongClickListener {
                val playlistOptionsDialog = PlaylistOptionsBottomSheet(
                    playlistId = playlist.id!!,
                    playlistName = playlist.name!!,
                    isOwner = true
                )
                playlistOptionsDialog.show(
                    childFragmentManager,
                    PlaylistOptionsBottomSheet::class.java.name
                )
                true
            }
        }
    }

    private fun deletePlaylist(activity: Activity, id: String, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = try {
                RetrofitInstance.authApi.deletePlaylist(
                    PreferenceHelper.getToken(),
                    PlaylistId(id)
                )
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launch
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                return@launch
            }
            try {
                if (response.message == "ok") {
                    playlists.removeAt(position)
                    activity.runOnUiThread {
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, itemCount)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
            }
        }
    }
}
