package com.github.libretube.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.github.libretube.R
import com.github.libretube.formatShort
import com.github.libretube.obj.Comment
import com.github.libretube.obj.CommentsPage
import com.github.libretube.util.RetrofitInstance
import com.squareup.picasso.Picasso
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class CommentsAdapter(
    private val videoId: String,
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsViewHolder>() {

    private val TAG = "CommentsAdapter"
    private var isLoading = false
    private var nextPage = ""
    private lateinit var repliesRecView: RecyclerView
    private lateinit var repliesPage: CommentsPage

    fun updateItems(newItems: List<Comment>) {
        var commentsSize = comments.size
        comments.addAll(newItems)
        notifyItemRangeInserted(commentsSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        var commentsView =
            LayoutInflater.from(parent.context).inflate(R.layout.comments_row, parent, false)
        return CommentsViewHolder(commentsView)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        holder.v.findViewById<TextView>(R.id.comment_infos).text =
            comments[position].author.toString() +
            " • " + comments[position].commentedTime.toString()
        holder.v.findViewById<TextView>(R.id.comment_text).text =
            comments[position].commentText.toString()
        val channelImage = holder.v.findViewById<ImageView>(R.id.commentor_image)
        Picasso.get().load(comments[position].thumbnail).fit().centerCrop().into(channelImage)
        holder.v.findViewById<TextView>(R.id.likes_textView).text =
            comments[position].likeCount?.toLong().formatShort()
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
        repliesRecView = holder.v.findViewById(R.id.replies_recView)
        repliesRecView.layoutManager = LinearLayoutManager(holder.v.context)
        val repliesAdapter = RepliesAdapter(CommentsPage().comments)
        repliesRecView.adapter = repliesAdapter
        holder.v.setOnClickListener {
            Log.e("clicekd", "clicked")
            if (repliesAdapter.itemCount == 0) {
                nextPage = comments[position].repliesPage!!
                fetchReplies(repliesAdapter)
                // repliesAdapter.updateItems(repliesPage.comments)
            } else {
                repliesAdapter.clear()
            }
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    private fun fetchReplies(repliesAdapter: RepliesAdapter) {
        CoroutineScope(Dispatchers.Main).launch {
            if (!isLoading && nextPage != null) {
                isLoading = true
                try {
                    repliesPage = RetrofitInstance.api.getCommentsNextPage(videoId!!, nextPage!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                }
                // nextPage = if (repliesPage.nextpage!! != null) repliesPage.nextpage!! else ""
                repliesAdapter.updateItems(repliesPage.comments)
                isLoading = false
            }
        }
    }
}

class CommentsViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
