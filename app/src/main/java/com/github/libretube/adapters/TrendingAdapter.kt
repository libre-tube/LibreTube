package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.formatShort
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.StreamItem
import com.squareup.picasso.Picasso

class TrendingAdapter(private val videoFeed: List<StreamItem>) : RecyclerView.Adapter<CustomViewHolder>() {
    override fun getItemCount(): Int {
        return videoFeed.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.trending_row, parent, false)
        return CustomViewHolder(cell)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.v.findViewById<TextView>(R.id.textView_title).text = trending.title
        holder.v.findViewById<TextView>(R.id.textView_channel).text = trending.uploaderName + " • " + trending.views.formatShort() + " • " + DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.thumbnail)
        holder.v.findViewById<TextView>(R.id.thumbnail_duration).text = DateUtils.formatElapsedTime(trending.duration!!)
        val channelImage = holder.v.findViewById<ImageView>(R.id.channel_image)
        channelImage.setOnClickListener {
            val activity = holder.v.context as MainActivity
            val bundle = bundleOf("channel_id" to trending.uploaderUrl)
            activity.navController.navigate(R.id.channel, bundle)
            try {
                val mainMotionLayout = activity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (mainMotionLayout.progress == 0.toFloat()) {
                    mainMotionLayout.transitionToEnd()
                    activity.findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
                }
            } catch (e: Exception) {
            }
        }
        if (trending.thumbnail!!.isEmpty()) {
        } else {
            Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        }
        if (trending.uploaderAvatar!!.isEmpty()) {
        } else {
            Picasso.get().load(trending.uploaderAvatar).into(channelImage)
        }

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
    }
}
class CustomViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
