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
        val cell = layoutInflater.inflate(R.layout.video_channel_row, parent, false)
        return PlaylistViewHolder(cell)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val streamItem = videoFeed[position]
        val thumbnailImage = holder.view.findViewById<ImageView>(R.id.channel_thumbnail)

        Picasso.get().load(streamItem.thumbnail).into(thumbnailImage)

        holder.view.findViewById<TextView>(R.id.channel_description).text = streamItem.title
        holder.view.findViewById<TextView>(R.id.channel_views).text = streamItem.uploaderName
        holder.view.findViewById<TextView>(R.id.channel_duration).text =
            DateUtils.formatElapsedTime(streamItem.duration!!)

        holder.view.setOnClickListener {
            val bundle = Bundle()
            val frag = PlayerFragment()
            val activity = holder.view.context as AppCompatActivity

            bundle.putString("videoId", streamItem.url!!.replace("/watch?v=", ""))
            frag.arguments = bundle
            activity.supportFragmentManager.beginTransaction()
                .remove(PlayerFragment())
                .commit()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, frag)
                .commitNow()
        }
    }
}

class PlaylistViewHolder(val view: View) : RecyclerView.ViewHolder(view)