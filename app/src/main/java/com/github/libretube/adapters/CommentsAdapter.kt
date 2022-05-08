package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.formatShort
import com.github.libretube.obj.Comment
import com.github.libretube.obj.StreamItem
import com.squareup.picasso.Picasso

class CommentsAdapter(private val comments: MutableList<Comment>):  RecyclerView.Adapter<ViewHolder>(){

    fun updateItems(newItems: List<Comment>){
        comments.addAll(newItems)
        notifyItemRangeInserted(comments.size, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
         var commentsView = LayoutInflater.from(parent.context).inflate(R.layout.comments_row, parent, false)
        return ViewHolder(commentsView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.v.findViewById<TextView>(R.id.comment_infos).text = comments[position].author.toString() + " • " + comments[position].commentedTime.toString()
        holder.v.findViewById<TextView>(R.id.comment_text).text = comments[position].commentText.toString()
        val thumbnailImage = holder.v.findViewById<ImageView>(R.id.commentor_image)
        Picasso.get().load(comments[position].thumbnail).into(thumbnailImage)
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
    }

    override fun getItemCount(): Int {
        return comments.size
    }

}

class ViewHolder(val v: View): RecyclerView.ViewHolder(v){
    init {
    }
}