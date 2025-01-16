package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.github.libretube.R
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

class CommentPagingAdapter(
    private val fragment: Fragment?,
    private val videoId: String,
    private val channelAvatar: String?,
    private val isRepliesAdapter: Boolean = false,
    private val handleLink: ((url: String) -> Unit)?,
    private val dismiss: () -> Unit
) : PagingDataAdapter<Comment, CommentsViewHolder>(CommentCallback) {
    private var clickEventConsumedByLinkHandler = false

    private fun navigateToReplies(comment: Comment) {
        if (clickEventConsumedByLinkHandler) {
            clickEventConsumedByLinkHandler = false
            return
        }

        val args = bundleOf(
            IntentData.videoId to videoId,
            IntentData.comment to comment,
            IntentData.channelAvatar to channelAvatar
        )
        fragment!!.parentFragmentManager.commit {
            replace<CommentsRepliesFragment>(R.id.commentFragContainer, args = args)
            addToBackStack(null)
        }
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = getItem(position)!!

        holder.binding.apply {
            commentAuthor.text = comment.author
            commentAuthor.setBackgroundResource(
                if (comment.channelOwner) R.drawable.comment_channel_owner_bg else 0
            )
            commentInfos.text = comment.commentedTime

            commentText.movementMethod = LinkMovementMethodCompat.getInstance()
            val linkHandler = LinkHandler {
                clickEventConsumedByLinkHandler = true
                handleLink?.invoke(it)
            }
            commentText.text = comment.commentText?.replace("</a>", "</a> ")
                ?.parseAsHtml(tagHandler = HtmlParser(linkHandler))

            ImageHelper.loadImage(comment.thumbnail, commentorImage, true)
            likesTextView.text = comment.likeCount.formatShort()

            if (comment.creatorReplied && !channelAvatar.isNullOrBlank()) {
                ImageHelper.loadImage(channelAvatar, creatorReplyImageView, true)
                creatorReplyImageView.isVisible = true
            }

            verifiedImageView.isVisible = comment.verified
            pinnedImageView.isVisible = comment.pinned
            heartedImageView.isVisible = comment.hearted
            repliesCount.isVisible = comment.repliesPage != null
            if (comment.replyCount > 0L) {
                repliesCount.text = comment.replyCount.formatShort()
            }

            commentorImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, comment.commentorUrl)
                dismiss()
            }

            if (isRepliesAdapter) {
                repliesCount.isGone = true

                // highlight the comment that is being replied to
                if (position == 0) {
                    root.setBackgroundColor(
                        ThemeHelper.getThemeColor(
                            root.context,
                            com.google.android.material.R.attr.colorSurface
                        )
                    )

                    root.updatePadding(top = 20)
                    root.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = 20 }
                } else {
                    root.background = AppCompatResources.getDrawable(
                        root.context,
                        R.drawable.rounded_ripple
                    )
                }
            }

            if (!isRepliesAdapter && comment.repliesPage != null) {
                val onClickListener = View.OnClickListener { navigateToReplies(comment) }
                root.setOnClickListener(onClickListener)
                commentText.setOnClickListener(onClickListener)
            }
            root.setOnLongClickListener {
                ClipboardHelper.save(
                    root.context,
                    text = comment.commentText.orEmpty().parseAsHtml().toString(),
                    notify = true
                )
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CommentsRowBinding.inflate(layoutInflater, parent, false)
        return CommentsViewHolder(binding)
    }
}

private object CommentCallback : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem.commentId == newItem.commentId
    }

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = true
}
