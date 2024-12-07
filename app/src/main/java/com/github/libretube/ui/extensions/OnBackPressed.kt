package com.github.libretube.ui.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

fun Fragment.setOnBackPressed(action: OnBackPressedCallback.() -> Unit): OnBackPressedCallback {
    val callback =  object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            action()
        }
    }

    return setOnBackPressed(callback)
}

fun Fragment.setOnBackPressed(callback: OnBackPressedCallback): OnBackPressedCallback {
    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        callback
    )

    return callback
}