package com.github.libretube.ui.sheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.github.libretube.R
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.ui.fragments.CommentsMainFragment
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.PlayerViewModel

class CommentsSheet : UndimmedBottomSheet() {
    lateinit var binding: CommentsSheetBinding
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val commentsViewModel: CommentsViewModel by activityViewModels()

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

        commentsViewModel.commentsSheetDismiss = this::dismiss

        binding.apply {
            dragHandle.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    dragHandle.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // limit the recyclerview height to not cover the video
                    binding.standardBottomSheet.layoutParams =
                        binding.commentFragContainer.layoutParams.apply {
                            height = playerViewModel.maxSheetHeightPx
                        }
                }
            })

            btnBack.setOnClickListener {
                if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                }
            }

            btnClose.setOnClickListener { dismiss() }
        }

        childFragmentManager.commit {
            replace<CommentsMainFragment>(R.id.commentFragContainer)
        }

        commentsViewModel.setCommentSheetExpand(true)
        commentsViewModel.commentSheetExpand.observe(viewLifecycleOwner) {
            when (it) {
                true -> expand()
                false -> expand(true)
                else -> dismiss()
            }
        }
    }

    override fun getSheetMaxHeightPx() = playerViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    fun updateFragmentInfo(showBackButton: Boolean, title: String) {
        binding.btnBack.isVisible = showBackButton
        binding.commentsTitle.text = title
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        commentsViewModel.commentsSheetDismiss = null
    }
}
