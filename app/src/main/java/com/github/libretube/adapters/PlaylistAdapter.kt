package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.fragment.KEY_VIDEO_ID
import com.github.libretube.fragment.PlayerFragment
import com.github.libretube.model.StreamItem
import com.squareup.picasso.Picasso

class PlaylistAdapter(private val videoFeed: MutableList<StreamItem>) :
    RecyclerView.Adapter<PlaylistViewHolder>() {
    override fun getItemCount(): Int {
        return videoFeed.size
    }

    fun updateItems(newItems: List<StreamItem>) {
        videoFeed.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val videoChannelRowBinding = VideoChannelRowBinding.inflate(layoutInflater, parent, false)
        return PlaylistViewHolder(videoChannelRowBinding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) =
        with(holder.videoChannelRowBinding) {
            val streamItem = videoFeed[position]
            Picasso.get().load(streamItem.thumbnail).into(channelThumbnail)

            tvChannelDescription.text = streamItem.title
            channelViews.text = streamItem.uploaderName
            channelDuration.text =
                DateUtils.formatElapsedTime(streamItem.duration!!)

            root.setOnClickListener {
                val bundle = Bundle()
                val playerFragment = PlayerFragment()
                val activity = root.context as AppCompatActivity

                bundle.putString(KEY_VIDEO_ID, streamItem.url!!.replace("/watch?v=", ""))
                playerFragment.arguments = bundle

                activity.supportFragmentManager.beginTransaction()
                    .remove(PlayerFragment())
                    .commit()
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, playerFragment)
                    .commitNow()
            }
        }
}

class PlaylistViewHolder(val videoChannelRowBinding: VideoChannelRowBinding) :
    RecyclerView.ViewHolder(videoChannelRowBinding.root)
