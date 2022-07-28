package com.github.libretube.adapters

import android.app.Activity
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.PlaylistRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.obj.PlaylistId
import com.github.libretube.obj.StreamItem
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.setWatchProgressLength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PlaylistAdapter(
    private val videoFeed: MutableList<StreamItem>,
    private val playlistId: String,
    private val isOwner: Boolean,
    private val activity: Activity,
    private val childFragmentManager: FragmentManager
) : RecyclerView.Adapter<PlaylistViewHolder>() {
    private val TAG = "PlaylistAdapter"

    override fun getItemCount(): Int {
        return videoFeed.size
    }

    fun updateItems(newItems: List<StreamItem>) {
        videoFeed.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PlaylistRowBinding.inflate(layoutInflater, parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val streamItem = videoFeed[position]
        holder.binding.apply {
            playlistTitle.text = streamItem.title
            playlistDescription.text = streamItem.uploaderName
            thumbnailDuration.text = DateUtils.formatElapsedTime(streamItem.duration!!)
            ConnectionHelper.loadImage(streamItem.thumbnail, playlistThumbnail)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, streamItem.url, playlistId)
            }
            val videoId = streamItem.url!!.replace("/watch?v=", "")
            root.setOnLongClickListener {
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }

            if (isOwner) {
                deletePlaylist.visibility = View.VISIBLE
                deletePlaylist.setOnClickListener {
                    val token = PreferenceHelper.getToken()
                    removeFromPlaylist(token, position)
                }
            }
            watchProgress.setWatchProgressLength(videoId, streamItem.duration!!)
        }
    }

    private fun removeFromPlaylist(token: String, position: Int) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                val response = try {
                    RetrofitInstance.authApi.removeFromPlaylist(
                        token,
                        PlaylistId(playlistId = playlistId, index = position)
                    )
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launch
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launch
                } finally {
                }
                try {
                    if (response.message == "ok") {
                        Log.d(TAG, "deleted!")
                        videoFeed.removeAt(position)
                        // FIXME: This needs to run on UI thread?
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

class PlaylistViewHolder(val binding: PlaylistRowBinding) : RecyclerView.ViewHolder(binding.root)
