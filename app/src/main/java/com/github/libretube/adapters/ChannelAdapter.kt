package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.formatShort
import com.squareup.picasso.Picasso

class ChannelAdapter(
    private val videoFeed: MutableList<StreamItem>,
    private val childFragmentManager: FragmentManager
) :
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
        val binding = VideoChannelRowBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.binding.apply {
            channelDescription.text = trending.title
            channelViews.text =
                trending.views.formatShort() + " • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            channelDuration.text =
                DateUtils.formatElapsedTime(trending.duration!!)
            ConnectionHelper.loadImage(trending.thumbnail, channelThumbnail)
            root.setOnClickListener {
                var bundle = Bundle()
                bundle.putString("videoId", trending.url!!.replace("/watch?v=", ""))
                var frag = PlayerFragment()
                frag.arguments = bundle
                val activity = root.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .remove(PlayerFragment())
                    .commit()
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, frag)
                    .commitNow()
            }
            root.setOnLongClickListener {
                val videoId = trending.url!!.replace("/watch?v=", "")
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, VideoOptionsDialog.TAG)
                true
            }
        }
    }
}

class ChannelViewHolder(val binding: VideoChannelRowBinding) : RecyclerView.ViewHolder(binding.root)
