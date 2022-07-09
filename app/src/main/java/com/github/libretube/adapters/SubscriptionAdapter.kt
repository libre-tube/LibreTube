package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.formatShort
import com.squareup.picasso.Picasso

class SubscriptionAdapter(
    private val videoFeed: List<StreamItem>,
    private val childFragmentManager: FragmentManager
) : RecyclerView.Adapter<SubscriptionViewHolder>() {
    private val TAG = "SubscriptionAdapter"

    var i = 0
    override fun getItemCount(): Int {
        return i
    }

    fun updateItems() {
        i += 10
        if (i > videoFeed.size) {
            i = videoFeed.size
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.binding.apply {
            textViewTitle.text = trending.title
            textViewChannel.text =
                trending.uploaderName + " • " +
                trending.views.formatShort() + " • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            if (trending.duration != -1L) {
                thumbnailDuration.text = DateUtils.formatElapsedTime(trending.duration!!)
            } else {
                thumbnailDuration.text = root.context.getString(R.string.live)
                thumbnailDuration.setBackgroundColor(R.attr.colorPrimaryDark)
            }
            channelImage.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf("channel_id" to trending.uploaderUrl)
                activity.navController.navigate(R.id.channelFragment, bundle)
                try {
                    val mainMotionLayout =
                        activity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                    if (mainMotionLayout.progress == 0.toFloat()) {
                        mainMotionLayout.transitionToEnd()
                        activity.findViewById<MotionLayout>(R.id.playerMotionLayout)
                            .transitionToEnd()
                    }
                } catch (e: Exception) {
                }
            }
            Picasso.get().load(trending.thumbnail).into(thumbnail)
            Picasso.get().load(trending.uploaderAvatar).into(channelImage)
            root.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("videoId", trending.url!!.replace("/watch?v=", ""))
                val frag = PlayerFragment()
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

class SubscriptionViewHolder(val binding: TrendingRowBinding) :
    RecyclerView.ViewHolder(binding.root)
