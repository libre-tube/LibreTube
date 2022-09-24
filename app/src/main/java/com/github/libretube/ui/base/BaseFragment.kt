package com.github.libretube.ui.base

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    fun runOnUiThread(action: () -> Unit) {
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
