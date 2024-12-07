package com.github.libretube.ui.extensions

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

fun Fragment.setOnBackPressed(action: OnBackPressedCallback.() -> Unit): OnBackPressedCallback {
    val callback =  object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            action()
        }
    }

    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        callback
       )

    return callback
}