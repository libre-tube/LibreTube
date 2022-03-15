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
import com.github.libretube.activity.MainActivity
import com.github.libretube.formatShort
import com.github.libretube.fragment.PlayerFragment
import com.github.libretube.model.StreamItem
import com.squareup.picasso.Picasso

class TrendingAdapter(private val videoFeed: List<StreamItem>) :
    RecyclerView.Adapter<CustomViewHolder>() {
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
        val thumbnailImage = holder.view.findViewById<ImageView>(R.id.thumbnail)
        val channelImage = holder.view.findViewById<ImageView>(R.id.channel_image)

        holder.view.findViewById<TextView>(R.id.textView_title).text = trending.title
        holder.view.findViewById<TextView>(R.id.textView_channel).text =
            trending.uploaderName + " • " + trending.views.formatShort() + " • " + DateUtils.getRelativeTimeSpanString(
                trending.uploaded!!
            )
        holder.view.findViewById<TextView>(R.id.thumbnail_duration).text =
            DateUtils.formatElapsedTime(trending.duration!!)

        channelImage.setOnClickListener {
            val activity = holder.view.context as MainActivity
            val bundle = bundleOf("channel_id" to trending.uploaderUrl)
            activity.navController.navigate(R.id.channelFragment, bundle)
            try {
                val mainMotionLayout = activity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (mainMotionLayout.progress == 0.toFloat()) {
                    mainMotionLayout.transitionToEnd()
                    activity.findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
                }
            } catch (e: Exception) {
                // TODO: Handle exception
            }
        }
        if (trending.thumbnail!!.isNotEmpty()) {
            Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        }

        if (trending.uploaderAvatar!!.isNotEmpty()) {
            Picasso.get().load(trending.uploaderAvatar).into(channelImage)
        }

        holder.view.setOnClickListener {
            val bundle = Bundle()
            val frag = PlayerFragment()
            val activity = holder.view.context as AppCompatActivity

            bundle.putString("videoId", trending.url!!.replace("/watch?v=", ""))
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

class CustomViewHolder(val view: View) : RecyclerView.ViewHolder(view)
