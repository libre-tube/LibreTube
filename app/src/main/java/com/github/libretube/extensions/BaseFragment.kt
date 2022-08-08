package com.github.libretube.extensions

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    fun runOnUiThread(action: () -> Unit) {
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
