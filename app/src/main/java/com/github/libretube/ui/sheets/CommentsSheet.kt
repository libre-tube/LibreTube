package com.github.libretube.ui.sheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.github.libretube.R
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.ui.fragments.CommentsMainFragment
import com.github.libretube.ui.models.CommentsViewModel

class CommentsSheet : ExpandedBottomSheet() {
    lateinit var binding: CommentsSheetBinding
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
                            height = commentsViewModel.maxHeight
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

        childFragmentManager.apply {
            beginTransaction()
                .replace(R.id.commentFragContainer, CommentsMainFragment())
                .commit()
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

    fun updateFragmentInfo(showBackButton: Boolean, title: String) {
        binding.btnBack.isVisible = showBackButton
        binding.commentsTitle.text = title
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        commentsViewModel.commentsSheetDismiss = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // BottomSheetDialogFragment passthrough user outside touch event
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.touch_outside)?.apply {
                setOnTouchListener { v, event ->
                    event.setLocation(event.rawX - v.x, event.rawY - v.y)
                    activity?.dispatchTouchEvent(event)
                    v.performClick()
                    false
                }
            }
        }

        dialog.apply {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (childFragmentManager.backStackEntryCount > 0) {
                        childFragmentManager.popBackStack()
                        return@setOnKeyListener true
                    }
                }
                return@setOnKeyListener false
            }

            window?.let {
                it.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }

            setCanceledOnTouchOutside(false)
        }

        return dialog
    }
}
