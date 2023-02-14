package com.github.libretube.ui.sheets

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class ExpandedBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) return dialog

        dialog.setOnShowListener {
            (it as BottomSheetDialog).let { d ->
                (d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?)?.let { fl ->
                    BottomSheetBehavior.from(fl).state =
                        BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        return dialog
    }

    fun show(fragmentManager: FragmentManager) = show(
        fragmentManager,
        null
    )

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // ensure that the sheet doesn't hide parts of the video
        dialog?.dismiss()
    }
}
