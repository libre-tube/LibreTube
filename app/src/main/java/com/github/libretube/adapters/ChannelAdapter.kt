package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
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
import com.github.libretube.obj.StreamItem
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
        val cell = layoutInflater.inflate(R.layout.video_channel_row, parent, false)
        return ChannelViewHolder(cell)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.v.findViewById<TextView>(R.id.channel_description).text = trending.title
        holder.v.findViewById<TextView>(R.id.channel_views).text =
            trending.views.formatShort() + " • " +
            DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
        holder.v.findViewById<TextView>(R.id.channel_duration).text =
            DateUtils.formatElapsedTime(trending.duration!!)
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.channel_thumbnail)
        Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        holder.v.setOnClickListener {
            var bundle = Bundle()
            bundle.putString("videoId", trending.url!!.replace("/watch?v=", ""))
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
            val videoId = trending.url!!.replace("/watch?v=", "")
            VideoOptionsDialog(videoId, holder.v.context)
                .show(childFragmentManager, VideoOptionsDialog.TAG)
            true
        }
    }
}

class ChannelViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
