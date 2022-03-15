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
import com.github.libretube.MainActivity
import com.squareup.picasso.Picasso
import com.github.libretube.PlayerFragment
import com.github.libretube.R
import com.github.libretube.obj.StreamItem
import com.github.libretube.formatShort

class SubscriptionAdapter(private val videoFeed: List<StreamItem>) :
    RecyclerView.Adapter<SubscriptionViewHolder>() {
    var amountOfItems = 0

    override fun getItemCount(): Int {
        return amountOfItems
    }

    fun updateItems() {
        amountOfItems += 10
        if (amountOfItems > videoFeed.size)
            amountOfItems = videoFeed.size
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.trending_row, parent, false)
        return SubscriptionViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
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
            activity.navController.navigate(R.id.channel, bundle)
            try {
                val mainMotionLayout = activity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (mainMotionLayout.progress == 0.toFloat()) {
                    mainMotionLayout.transitionToEnd()
                    activity.findViewById<MotionLayout>(R.id.playerMotionLayout).transitionToEnd()
                }
            } catch (e: Exception) {
                // TODO: handle exception
            }
        }
        Picasso.get().load(trending.thumbnail).into(thumbnailImage)
        Picasso.get().load(trending.uploaderAvatar).into(channelImage)
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

class SubscriptionViewHolder(val view: View) : RecyclerView.ViewHolder(view)