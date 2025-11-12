package com.github.libretube.ui.sheets

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import com.github.libretube.ui.models.CommonPlayerViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

abstract class ExpandablePlayerSheet(@LayoutRes layoutResId: Int) :
    UndimmedBottomSheet(layoutResId) {
    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (commonPlayerViewModel.isFullscreen.value == true) {
            // prevent an issue where swiping outside of the bottom sheet would make
            // the app unresponsive by disabling slide actions to dismiss the bottom sheet in fullscreen
            dialog.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet =
                    bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                        ?: return@setOnShowListener

                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismissAllowingStateLoss()
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                })
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commonPlayerViewModel.setSheetExpand(true)
        commonPlayerViewModel.sheetExpand.observe(viewLifecycleOwner) {
            when (it) {
                true -> expand()
                false -> expand(true)
                else -> dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commonPlayerViewModel.setSheetExpand(null)
    }
}