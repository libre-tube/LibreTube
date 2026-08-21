package com.github.libretube.ui.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment that manages ViewBinding lifecycle automatically.
 *
 * Subclasses must call [setBinding] in [onViewCreated] with the binding instance.
 * The binding is automatically cleared in [onDestroyView].
 *
 * Usage:
 * ```
 * class MyFragment : BaseBindingFragment<FragmentMyBinding>(R.layout.fragment_my) {
 *     override fun setBinding(view: View) {
 *         _binding = FragmentMyBinding.bind(view)
 *     }
 * }
 * ```
 */
abstract class BaseBindingFragment<VB : ViewBinding>(
    @LayoutRes layoutResId: Int
) : Fragment(layoutResId) {

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

    /**
     * Allow subclasses to set the binding directly (for use in setBinding implementations).
     */
    protected fun setBindingDirect(binding: VB) {
        _binding = binding
    }
}
