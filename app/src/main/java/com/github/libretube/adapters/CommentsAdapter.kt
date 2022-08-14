package com.github.libretube.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.CommentsRowBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.Comment
import com.github.libretube.obj.CommentsPage
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.formatShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class CommentsAdapter(
    private val videoId: String,
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsViewHolder>() {

    private var isLoading = false
    private var nextpage = ""
    private var repliesPage = CommentsPage()

    fun updateItems(newItems: List<Comment>) {
        val commentsSize = comments.size
        comments.addAll(newItems)
        notifyItemRangeInserted(commentsSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CommentsRowBinding.inflate(layoutInflater, parent, false)
        return CommentsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.apply {
            commentInfos.text =
                comment.author.toString() +
                " • " + comment.commentedTime.toString()
            commentText.text =
                comment.commentText.toString()
            ConnectionHelper.loadImage(comment.thumbnail, commentorImage)
            likesTextView.text =
                comment.likeCount?.toLong().formatShort()
            if (comment.verified == true) {
                verifiedImageView.visibility = View.VISIBLE
            }
            if (comment.pinned == true) {
                pinnedImageView.visibility = View.VISIBLE
            }
            if (comment.hearted == true) {
                heartedImageView.visibility = View.VISIBLE
            }
            commentorImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, comment.commentorUrl)
            }
            repliesRecView.layoutManager = LinearLayoutManager(root.context)
            val repliesAdapter = RepliesAdapter(CommentsPage().comments)
            repliesRecView.adapter = repliesAdapter
            root.setOnClickListener {
                if (repliesAdapter.itemCount == 0) {
                    if (comment.repliesPage != null) {
                        nextpage = comment.repliesPage
                        fetchReplies(nextpage, repliesAdapter)
                    } else {
                        Toast.makeText(root.context, R.string.no_replies, Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    repliesAdapter.clear()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    private fun fetchReplies(nextPage: String, repliesAdapter: RepliesAdapter) {
        CoroutineScope(Dispatchers.Main).launch {
            if (!isLoading) {
                isLoading = true
                try {
                    repliesPage = RetrofitInstance.api.getCommentsNextPage(videoId, nextPage)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                }
                repliesAdapter.updateItems(repliesPage.comments)
                isLoading = false
            }
        }
    }
}

class CommentsViewHolder(val binding: CommentsRowBinding) : RecyclerView.ViewHolder(binding.root)
