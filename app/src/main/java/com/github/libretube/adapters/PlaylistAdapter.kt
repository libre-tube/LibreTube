package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.StreamItem
import com.squareup.picasso.Picasso

class PlaylistAdapter(private val videoFeed: MutableList<StreamItem>) : RecyclerView.Adapter<PlaylistViewHolder>() {
    override fun getItemCount(): Int {
        return videoFeed.size
    }
    fun updateItems(newItems: List<StreamItem>) {
        videoFeed.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.video_channel_row, parent, false)
        return PlaylistViewHolder(cell)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val streamItem = videoFeed[position]
        holder.v.findViewById<TextView>(R.id.channel_description).text = streamItem.title
        holder.v.findViewById<TextView>(R.id.channel_views).text = streamItem.uploaderName
        holder.v.findViewById<TextView>(R.id.channel_duration).text = DateUtils.formatElapsedTime(streamItem.duration!!)
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.channel_thumbnail)
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
    }
}
class PlaylistViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
