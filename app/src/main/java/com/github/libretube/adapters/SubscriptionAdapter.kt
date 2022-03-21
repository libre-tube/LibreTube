package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.activity.MainActivity
import com.squareup.picasso.Picasso
import com.github.libretube.R
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.model.StreamItem
import com.github.libretube.formatShort
import com.github.libretube.fragment.KEY_CHANNEL_ID
import com.github.libretube.fragment.KEY_VIDEO_ID
import com.github.libretube.fragment.PlayerFragment

class SubscriptionAdapter(private val videoFeed: List<StreamItem>) :
    RecyclerView.Adapter<SubscriptionViewHolder>() {
    private var amountOfItems = 0

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
        val trendingRowBinding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionViewHolder(trendingRowBinding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) =
        with(holder.trendingRowBinding) {
            val trending = videoFeed[position]

            textViewTitle.text = trending.title
            textViewChannel.text =
                trending.uploaderName + " • " + trending.views.formatShort() + " • " + DateUtils.getRelativeTimeSpanString(
                    trending.uploaded!!
                )
            thumbnailDuration.text =
                DateUtils.formatElapsedTime(trending.duration!!)

            ivChannel.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf(KEY_CHANNEL_ID to trending.uploaderUrl)
                activity.navController.navigate(R.id.channelFragment, bundle)
                try {
                    val mainMotionLayout = activity.findViewById<MotionLayout>(R.id.mlMain)
                    if (mainMotionLayout.progress == 0.toFloat()) {
                        mainMotionLayout.transitionToEnd()
                        activity.findViewById<MotionLayout>(R.id.playerMotionLayout)
                            .transitionToEnd()
                    }
                } catch (e: Exception) {
                    // TODO: handle exception
                }
            }
            Picasso.get().apply {
                load(trending.thumbnail).into(thumbnail)
                load(trending.uploaderAvatar).into(ivChannel)
            }

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

class SubscriptionViewHolder(val trendingRowBinding: TrendingRowBinding) :
    RecyclerView.ViewHolder(trendingRowBinding.root)
