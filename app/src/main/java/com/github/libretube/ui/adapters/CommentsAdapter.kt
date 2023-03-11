package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.obj.Comment
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.CommentsRowBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.ui.fragments.CommentsRepliesFragment
import com.github.libretube.ui.viewholders.CommentsViewHolder
import com.github.libretube.util.HtmlParser
import com.github.libretube.util.LinkHandler
import com.github.libretube.util.TextUtils
import kotlinx.serialization.encodeToString

class CommentsAdapter(
    private val fragment: Fragment?,
    private val videoId: String,
    private val comments: MutableList<Comment>,
    private val isRepliesAdapter: Boolean = false,
    private val handleLink: ((url: String) -> Unit)?,
    private val dismiss: () -> Unit
) : RecyclerView.Adapter<CommentsViewHolder>() {
    fun clear() {
        val size: Int = comments.size
        comments.clear()
        notifyItemRangeRemoved(0, size)
    }

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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.apply {
            commentInfos.text = comment.author + TextUtils.SEPARATOR + comment.commentedTime

            commentText.movementMethod = LinkMovementMethod.getInstance()
            commentText.text = comment.commentText
                ?.parseAsHtml(tagHandler = HtmlParser(LinkHandler(handleLink ?: {})))

            ImageHelper.loadImage(comment.thumbnail, commentorImage)
            likesTextView.text = comment.likeCount.formatShort()

            if (comment.verified) verifiedImageView.visibility = View.VISIBLE
            if (comment.pinned) pinnedImageView.visibility = View.VISIBLE
            if (comment.hearted) heartedImageView.visibility = View.VISIBLE
            if (comment.repliesPage != null) repliesCount.visibility = View.VISIBLE
            if (comment.replyCount > 0L) {
                repliesCount.text = comment.replyCount.formatShort()
            }

            commentorImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, comment.commentorUrl)
                dismiss.invoke()
            }

            if (isRepliesAdapter) {
                repliesCount.visibility = View.GONE

                // highlight the comment that is being replied to
                if (comment == comments.firstOrNull()) {
                    root.setBackgroundColor(
                        ThemeHelper.getThemeColor(root.context, R.attr.colorSurface)
                    )
                    root.updatePadding(top = 20)
                    root.updateLayoutParams<MarginLayoutParams> { bottomMargin = 20 }
                } else {
                    root.background = AppCompatResources.getDrawable(
                        root.context,
                        R.drawable.rounded_ripple
                    )
                }
            }

            if (!isRepliesAdapter && comment.repliesPage != null) {
                root.setOnClickListener {
                    val args = bundleOf(
                        IntentData.videoId to videoId,
                        IntentData.comment to JsonHelper.json.encodeToString(comment)
                    )
                    fragment!!.parentFragmentManager.commit {
                        replace<CommentsRepliesFragment>(R.id.commentFragContainer, args = args)
                        addToBackStack(null)
                    }
                }
            }
            root.setOnLongClickListener {
                ClipboardHelper.save(
                    root.context,
                    comment.commentText.orEmpty().parseAsHtml().toString()
                )
                Toast.makeText(root.context, R.string.copied, Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}
