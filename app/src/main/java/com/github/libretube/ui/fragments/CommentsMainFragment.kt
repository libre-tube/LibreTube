package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.models.CommentsViewModel

class CommentsMainFragment : Fragment() {
    private lateinit var binding: FragmentCommentsBinding
    private lateinit var commentsAdapter: CommentsAdapter

    private val viewModel: CommentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.commentsRV.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.setItemViewCacheSize(20)

        binding.commentsRV.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.commentsRV.canScrollVertically(1)) {
                    viewModel.fetchNextComments()
                }
            }

        commentsAdapter = CommentsAdapter(
            this,
            viewModel.videoId!!,
            viewModel.commentsPage.value?.comments.orEmpty().toMutableList()
        ) {
            viewModel.commentsSheetDismiss?.invoke()
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
            if (it.disabled) {
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
