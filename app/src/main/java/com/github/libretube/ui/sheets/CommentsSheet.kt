package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.extensions.toPixel
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.models.CommentsViewModel

class CommentsSheet : ExpandedBottomSheet() {
    private lateinit var binding: CommentsSheetBinding

    private lateinit var commentsAdapter: CommentsAdapter

    private val viewModel: CommentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CommentsSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dragHandle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.dragHandle.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // limit the recyclerview height to not cover the video
                binding.commentsRV.layoutParams = binding.commentsRV.layoutParams.apply {
                    height = viewModel.maxHeight - (binding.dragHandle.height + (20).toPixel().toInt())
                }
            }
        })

        binding.commentsRV.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.setItemViewCacheSize(20)

        binding.commentsRV.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.commentsRV.canScrollVertically(1)) {
                    viewModel.fetchNextComments()
                }
            }

        commentsAdapter = CommentsAdapter(
            viewModel.videoId!!,
            viewModel.commentsPage.value?.comments.orEmpty().toMutableList()
        ) {
            dialog?.dismiss()
        }
        binding.commentsRV.adapter = commentsAdapter

        if (viewModel.commentsPage.value?.comments.orEmpty().isEmpty()) {
            binding.progress.visibility = View.VISIBLE
            viewModel.fetchComments()
        }

        // listen for new comments to be loaded
        viewModel.commentsPage.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.progress.visibility = View.GONE
            if (it.disabled == true) {
                binding.errorTV.visibility = View.VISIBLE
                return@observe
            }
            if (it.comments.isEmpty()) {
                binding.errorTV.text = getString(R.string.no_comments_available)
                binding.errorTV.visibility = View.VISIBLE
                return@observe
            }
            commentsAdapter.updateItems(
                // only add the new comments to the recycler view
                it.comments.subList(commentsAdapter.itemCount, it.comments.size)
            )
        }
    }
}
