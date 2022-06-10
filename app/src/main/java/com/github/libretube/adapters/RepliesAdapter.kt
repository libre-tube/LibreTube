package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.github.libretube.R
import com.github.libretube.obj.Comment
import com.github.libretube.util.formatShort
import com.squareup.picasso.Picasso

class RepliesAdapter(
    private val replies: MutableList<Comment>
) : RecyclerView.Adapter<RepliesViewHolder>() {

    private val TAG = "RepliesAdapter"
    private var isLoading = false
    private var nextPage = ""

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
        var repliesView =
            LayoutInflater.from(parent.context).inflate(R.layout.replies_row, parent, false)
        return RepliesViewHolder(repliesView)
    }

    override fun onBindViewHolder(holder: RepliesViewHolder, position: Int) {
        holder.v.findViewById<TextView>(R.id.comment_infos).text =
            replies[position].author.toString() +
            " • " + replies[position].commentedTime.toString()
        holder.v.findViewById<TextView>(R.id.comment_text).text =
            replies[position].commentText.toString()
        val channelImage = holder.v.findViewById<ImageView>(R.id.commentor_image)
        Picasso.get().load(replies[position].thumbnail).fit().centerCrop().into(channelImage)
        holder.v.findViewById<TextView>(R.id.likes_textView).text =
            replies[position].likeCount?.toLong().formatShort()
        if (replies[position].verified == true) {
            holder.v.findViewById<ImageView>(R.id.verified_imageView).visibility = View.VISIBLE
        }
        if (replies[position].pinned == true) {
            holder.v.findViewById<ImageView>(R.id.pinned_imageView).visibility = View.VISIBLE
        }
        if (replies[position].hearted == true) {
            holder.v.findViewById<ImageView>(R.id.hearted_imageView).visibility = View.VISIBLE
        }
        channelImage.setOnClickListener {
            val activity = holder.v.context as MainActivity
            val bundle = bundleOf("channel_id" to replies[position].commentorUrl)
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
    }

    override fun getItemCount(): Int {
        return replies.size
    }
}

class RepliesViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
