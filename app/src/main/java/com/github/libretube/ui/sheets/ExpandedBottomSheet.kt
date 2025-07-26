package com.github.libretube.ui.sheets

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.PEEK_HEIGHT_AUTO
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class ExpandedBottomSheet(@LayoutRes layoutResId: Int) :
    BottomSheetDialogFragment(layoutResId) {
    private val bottomSheet: FrameLayout? get() = dialog?.findViewById(R.id.design_bottom_sheet)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) return dialog

        dialog.setOnShowListener { expand() }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        // remove internal padding from the bottomsheet
        // https://github.com/material-components/material-components-android/issues/3389#issuecomment-2049028605
        dialog?.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    fun show(fragmentManager: FragmentManager) = show(fragmentManager, null)

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // ensure that the sheet doesn't hide parts of the video
        dialog?.dismiss()
    }

    fun expand(collapse: Boolean = false) {
        bottomSheet?.let { fl ->
            val bottomSheetInfoBehavior = BottomSheetBehavior.from(fl)
            if (collapse) {
                bottomSheetInfoBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheetInfoBehavior.setPeekHeight(0, true)
            } else {
                bottomSheetInfoBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                bottomSheetInfoBehavior.setPeekHeight(PEEK_HEIGHT_AUTO, true)
            }
        }
    }
}
