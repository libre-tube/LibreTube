package com.github.libretube.ui.sheets

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class ExpandedBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            (it as BottomSheetDialog).let { d ->
                (d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?)?.let {
                    BottomSheetBehavior.from(it).state =
                        BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        return dialog
    }
}
