package com.github.libretube.ui.sheets

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams

/**
 * A bottom sheet that allows touches on its top/background
 */
abstract class UndimmedBottomSheet(@LayoutRes layoutResId: Int) : ExpandedBottomSheet(layoutResId) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getDragHandle().viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                getDragHandle().viewTreeObserver.removeOnGlobalLayoutListener(this)

                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // limit the recyclerview height to not cover the video
                    getBottomSheet().updateLayoutParams {
                        height = getSheetMaxHeightPx()
                    }
                }
            }
        })
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

    abstract fun getSheetMaxHeightPx(): Int
    abstract fun getDragHandle(): View
    abstract fun getBottomSheet(): FrameLayout
}
