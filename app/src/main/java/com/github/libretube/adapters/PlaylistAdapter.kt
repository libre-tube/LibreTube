package com.github.libretube.adapters

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.PlaylistId
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.RetrofitInstance
import com.squareup.picasso.Picasso
import java.io.IOException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
        val cell = layoutInflater.inflate(R.layout.playlist_row, parent, false)
        return PlaylistViewHolder(cell)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val streamItem = videoFeed[position]
        holder.v.findViewById<TextView>(R.id.playlist_title).text = streamItem.title
        holder.v.findViewById<TextView>(R.id.playlist_description).text = streamItem.uploaderName
        holder.v.findViewById<TextView>(R.id.playlist_duration).text =
            DateUtils.formatElapsedTime(streamItem.duration!!)
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.playlist_thumbnail)
        Picasso.get().load(streamItem.thumbnail).into(thumbnailImage)
        holder.v.setOnClickListener {
            var bundle = Bundle()
            bundle.putString("videoId", streamItem.url!!.replace("/watch?v=", ""))
            var frag = PlayerFragment()
            frag.arguments = bundle
            val activity = holder.v.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .remove(PlayerFragment())
                .commit()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, frag)
                .commitNow()
        }
        holder.v.setOnLongClickListener {
            val videoId = streamItem.url!!.replace("/watch?v=", "")
            VideoOptionsDialog(videoId, holder.v.context)
                .show(childFragmentManager, VideoOptionsDialog.TAG)
            true
        }

        if (isOwner) {
            val delete = holder.v.findViewById<ImageView>(R.id.delete_playlist)
            delete.visibility = View.VISIBLE
            delete.setOnClickListener {
                val sharedPref = holder.v.context.getSharedPreferences(
                    "token",
                    Context.MODE_PRIVATE
                )
                val token = sharedPref?.getString("token", "")!!
                removeFromPlaylist(token, position)
            }
        }
    }

    private fun removeFromPlaylist(token: String, position: Int) {
        fun run() {
            GlobalScope.launch {
                val response = try {
                    RetrofitInstance.api.removeFromPlaylist(
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

                        /*if(playlists.isEmpty()){
                            view.findViewById<ImageView>(R.id.boogh2).visibility=View.VISIBLE
                        }*/
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
        run()
    }
}

class PlaylistViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
