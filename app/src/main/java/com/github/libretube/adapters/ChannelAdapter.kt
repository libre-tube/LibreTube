package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.github.libretube.R
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.model.StreamItem
import com.github.libretube.formatShort
import com.github.libretube.fragment.KEY_VIDEO_ID
import com.github.libretube.fragment.PlayerFragment

class ChannelAdapter(private val videoFeed: MutableList<StreamItem>) :
    RecyclerView.Adapter<ChannelViewHolder>() {
    override fun getItemCount(): Int {
        return videoFeed.size
    }

    fun updateItems(newItems: List<StreamItem>) {
        videoFeed.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val videoChannelRowBinding = VideoChannelRowBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(videoChannelRowBinding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) =
        with(holder.videoChannelRowBinding) {
            val trending = videoFeed[position]
            Picasso.get().load(trending.thumbnail).into(channelThumbnail)

            tvChannelDescription.text = trending.title
            channelViews.text =
                trending.views.formatShort() + " • " + DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            channelDuration.text =
                DateUtils.formatElapsedTime(trending.duration!!)

            root.setOnClickListener {
                val bundle = Bundle()
                val playerFragment = PlayerFragment()
                val activity = root.context as AppCompatActivity

                bundle.putString(KEY_VIDEO_ID, trending.url!!.replace("/watch?v=", ""))
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

class ChannelViewHolder(val videoChannelRowBinding: VideoChannelRowBinding) :
    RecyclerView.ViewHolder(videoChannelRowBinding.root)
