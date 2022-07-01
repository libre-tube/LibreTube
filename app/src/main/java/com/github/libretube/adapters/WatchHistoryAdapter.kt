package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.databinding.WatchHistoryRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.WatchHistoryItem
import com.squareup.picasso.Picasso

class WatchHistoryAdapter(
    private val watchHistory: MutableList<WatchHistoryItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<WatchHistoryViewHolder>() {
    private val TAG = "WatchHistoryAdapter"
    private lateinit var binding: WatchHistoryRowBinding

    fun clear() {
        val size = watchHistory.size
        watchHistory.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = WatchHistoryRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val video = watchHistory[position]
        binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            uploadDate.text = video.uploadDate
            thumbnailDuration.text = DateUtils.formatElapsedTime(video.duration?.toLong()!!)
            Picasso.get().load(video.thumbnailUrl).into(thumbnail)
            Picasso.get().load(video.uploaderAvatar).into(channelImage)

            channelImage.setOnClickListener {
                val activity = holder.v.context as MainActivity
                val bundle = bundleOf("channel_id" to video.uploaderUrl)
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

            root.setOnClickListener {
                var bundle = Bundle()
                bundle.putString("videoId", video.videoId)
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
            root.setOnLongClickListener {
                VideoOptionsDialog(video.videoId!!, holder.v.context)
                    .show(childFragmentManager, VideoOptionsDialog.TAG)
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return watchHistory.size
    }
}

class WatchHistoryViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
