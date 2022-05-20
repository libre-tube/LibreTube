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
import com.github.libretube.formatShort
import com.github.libretube.obj.Comment
import com.squareup.picasso.Picasso

class CommentsAdapter(private val comments: MutableList<Comment>) : RecyclerView.Adapter<ViewHolder>() {

    fun updateItems(newItems: List<Comment>) {
        var commentsSize = comments.size
        comments.addAll(newItems)
        notifyItemRangeInserted(commentsSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var commentsView = LayoutInflater.from(parent.context).inflate(R.layout.comments_row, parent, false)
        return ViewHolder(commentsView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.v.findViewById<TextView>(R.id.comment_infos).text = comments[position].author.toString() + " • " + comments[position].commentedTime.toString()
        holder.v.findViewById<TextView>(R.id.comment_text).text = comments[position].commentText.toString()
        val channelImage = holder.v.findViewById<ImageView>(R.id.commentor_image)
        Picasso.get().load(comments[position].thumbnail).fit().centerCrop().into(channelImage)
        holder.v.findViewById<TextView>(R.id.likes_textView).text = comments[position].likeCount?.toLong().formatShort()
        if (comments[position].verified == true) {
            holder.v.findViewById<ImageView>(R.id.verified_imageView).visibility = View.VISIBLE
        }
        if (comments[position].pinned == true) {
            holder.v.findViewById<ImageView>(R.id.pinned_imageView).visibility = View.VISIBLE
        }
        if (comments[position].hearted == true) {
            holder.v.findViewById<ImageView>(R.id.hearted_imageView).visibility = View.VISIBLE
        }
        channelImage.setOnClickListener {
            val activity = holder.v.context as MainActivity
            val bundle = bundleOf("channel_id" to comments[position].commentorUrl)
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
        return comments.size
    }
}

class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
