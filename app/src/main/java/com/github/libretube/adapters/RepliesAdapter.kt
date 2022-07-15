package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.databinding.RepliesRowBinding
import com.github.libretube.obj.Comment
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.formatShort

class RepliesAdapter(
    private val replies: MutableList<Comment>
) : RecyclerView.Adapter<RepliesViewHolder>() {
    private val TAG = "RepliesAdapter"

    fun clear() {
        val size: Int = replies.size
        replies.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun updateItems(newItems: List<Comment>) {
        var repliesSize = replies.size
        replies.addAll(newItems)
        notifyItemRangeInserted(repliesSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepliesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RepliesRowBinding.inflate(layoutInflater, parent, false)
        return RepliesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RepliesViewHolder, position: Int) {
        holder.binding.apply {
            val reply = replies[position]
            commentInfos.text =
                reply.author.toString() +
                " • " + reply.commentedTime.toString()
            commentText.text =
                reply.commentText.toString()
            ConnectionHelper.loadImage(reply.thumbnail, commentorImage)
            likesTextView.text =
                reply.likeCount?.toLong().formatShort()
            if (reply.verified == true) {
                verifiedImageView.visibility = View.VISIBLE
            }
            if (reply.pinned == true) {
                pinnedImageView.visibility = View.VISIBLE
            }
            if (reply.hearted == true) {
                heartedImageView.visibility = View.VISIBLE
            }
            commentorImage.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf("channel_id" to reply.commentorUrl)
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
        }
    }

    override fun getItemCount(): Int {
        return replies.size
    }
}

class RepliesViewHolder(val binding: RepliesRowBinding) : RecyclerView.ViewHolder(binding.root)
