package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

abstract class ExpandablePlayerSheet(@LayoutRes layoutResId: Int) :
    UndimmedBottomSheet(layoutResId) {
    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commonPlayerViewModel.isFullscreen.observe(viewLifecycleOwner) {
            handleBottomSheetFullScreenState(isFullScreen = it)
        }
        commonPlayerViewModel.setSheetExpand(true)
        commonPlayerViewModel.sheetExpand.observe(viewLifecycleOwner) {
            when (it) {
                true -> expand()
                false -> expand(true)
                else -> dismiss()
            }
        }
    }
    private fun handleBottomSheetFullScreenState(isFullScreen: Boolean) {
        if (isFullScreen) {
            dialog?.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet =
                    bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

                bottomSheet?.let {
                    val behavior = BottomSheetBehavior.from(it)

                    behavior.addBottomSheetCallback(object :
                        BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            when (newState) {
                                BottomSheetBehavior.STATE_HIDDEN -> {
                                    dismissAllowingStateLoss()
                                    dismiss()
                                }
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        }
                    })
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonPlayerViewModel.setSheetExpand(null)
    }
}