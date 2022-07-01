package com.github.libretube.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.github.libretube.R
import com.github.libretube.databinding.CommentsRowBinding
import com.github.libretube.obj.Comment
import com.github.libretube.obj.CommentsPage
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.formatShort
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class CommentsAdapter(
    private val videoId: String,
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsViewHolder>() {
    private val TAG = "CommentsAdapter"
    private lateinit var binding: CommentsRowBinding

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
        binding = CommentsRowBinding.inflate(layoutInflater, parent, false)
        return CommentsViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = comments[position]
        binding.apply {
            commentInfos.text =
                comment.author.toString() +
                " • " + comment.commentedTime.toString()
            commentText.text =
                comment.commentText.toString()
            Picasso.get().load(comment.thumbnail).fit().centerCrop().into(commentorImage)
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
                val activity = holder.v.context as MainActivity
                val bundle = bundleOf("channel_id" to comment.commentorUrl)
                activity.navController.navigate(R.id.channel, bundle)
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
            repliesRecView.layoutManager = LinearLayoutManager(holder.v.context)
            val repliesAdapter = RepliesAdapter(CommentsPage().comments)
            repliesRecView.adapter = repliesAdapter
            root.setOnClickListener {
                if (repliesAdapter.itemCount == 0) {
                    if (comment.repliesPage != null) {
                        nextpage = comment.repliesPage
                        fetchReplies(nextpage, repliesAdapter)
                    } else {
                        Toast.makeText(holder.v.context, R.string.no_replies, Toast.LENGTH_SHORT)
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
                    Log.e(TAG, "IOException, you might not have internet connection")
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response," + e.response())
                }
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
