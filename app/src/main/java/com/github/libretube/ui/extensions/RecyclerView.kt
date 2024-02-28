package com.github.libretube.ui.extensions

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addOnBottomReachedListener(onBottomReached: () -> Unit) {
    viewTreeObserver.addOnScrollChangedListener {
        if (!canScrollVertically(1)) onBottomReached()
    }
}
