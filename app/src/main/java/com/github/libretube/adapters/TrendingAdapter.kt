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
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.formatShort

class TrendingAdapter(
    private val videoFeed: List<StreamItem>,
    private val childFragmentManager: FragmentManager
) : RecyclerView.Adapter<TrendingViewHolder>() {
    private val TAG = "TrendingAdapter"

    override fun getItemCount(): Int {
        return videoFeed.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return TrendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
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
            ConnectionHelper.loadImage(trending.thumbnail, thumbnail)
            ConnectionHelper.loadImage(trending.uploaderAvatar, channelImage)

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
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }
        }
    }
}

class TrendingViewHolder(val binding: TrendingRowBinding) : RecyclerView.ViewHolder(binding.root)
