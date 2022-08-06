package com.github.libretube.adapters

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.obj.PlaylistId
import com.github.libretube.obj.Playlists
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PlaylistsAdapter(
    private val playlists: MutableList<Playlists>,
    private val activity: Activity
) : RecyclerView.Adapter<PlaylistsViewHolder>() {
    val TAG = "PlaylistsAdapter"

    override fun getItemCount(): Int {
        return playlists.size
    }

    fun updateItems(newItems: List<Playlists>) {
        playlists.addAll(newItems)
        notifyDataSetChanged()
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
            Log.e(TAG, playlist.thumbnail.toString())
            if (playlist.thumbnail!!.split("/").size <= 4) {
                playlistThumbnail.setImageResource(R.drawable.ic_empty_playlist)
                playlistThumbnail.setBackgroundColor(R.attr.colorSurface)
            } else {
                ConnectionHelper.loadImage(playlist.thumbnail, playlistThumbnail)
            }
            playlistTitle.text = playlist.name
            deletePlaylist.setOnClickListener {
                val builder = MaterialAlertDialogBuilder(root.context)
                builder.setTitle(R.string.deletePlaylist)
                builder.setMessage(R.string.areYouSure)
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    PreferenceHelper.getToken()
                    deletePlaylist(playlist.id!!, position)
                }
                builder.setNegativeButton(R.string.cancel, null)
                builder.show()
            }
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, playlist.id)
            }
        }
    }

    private fun deletePlaylist(id: String, position: Int) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    RetrofitInstance.authApi.deletePlaylist(
                        PreferenceHelper.getToken(),
                        PlaylistId(id)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launch
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launch
                }
                try {
                    if (response.message == "ok") {
                        playlists.removeAt(position)
                        activity.runOnUiThread { notifyDataSetChanged() }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
        run()
    }
}

class PlaylistsViewHolder(val binding: PlaylistsRowBinding) : RecyclerView.ViewHolder(binding.root)
