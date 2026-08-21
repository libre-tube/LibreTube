package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding

/**
 * Base BottomSheet that manages ViewBinding lifecycle automatically.
 *
 * Subclasses must call [setBinding] in [onViewCreated] with the binding instance.
 * The binding is automatically cleared in [onDestroyView].
 */
abstract class BaseBindingBottomSheet<VB : ViewBinding>(
    @LayoutRes layoutResId: Int
) : ExpandedBottomSheet(layoutResId) {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected abstract fun setBinding(view: View)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
