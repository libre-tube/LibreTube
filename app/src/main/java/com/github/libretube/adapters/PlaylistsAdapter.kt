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
import com.squareup.picasso.Picasso
import com.github.libretube.PlayerFragment
import com.github.libretube.R
import com.github.libretube.obj.StreamItem
import com.github.libretube.formatShort
import com.github.libretube.obj.Playlists

class PlaylistsAdapter(private val playlists: MutableList<Playlists>): RecyclerView.Adapter<PlaylistsViewHolder>() {
    override fun getItemCount(): Int {
        return playlists.size
    }
    fun updateItems(newItems: List<Playlists>){
        playlists.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.playlists_row,parent,false)
        return PlaylistsViewHolder(cell)
    }

    override fun onBindViewHolder(holder: PlaylistsViewHolder, position: Int) {
        val playlist = playlists[position]

    }
}
class PlaylistsViewHolder(val v: View): RecyclerView.ViewHolder(v){
    init {
    }
}