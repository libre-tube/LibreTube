package com.github.libretube.ui.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addOnBottomReachedListener(
    prefetchDistance: Int = 0,
    onBottomReached: () -> Unit
) {
    require(prefetchDistance >= 0)

    if (prefetchDistance == 0) {
        viewTreeObserver.addOnScrollChangedListener {
            if (!canScrollVertically(1)) onBottomReached()
        }
        return
    }

    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val itemCount = recyclerView.adapter?.itemCount ?: return
            if (layoutManager.findLastVisibleItemPosition() >= itemCount - prefetchDistance - 1) {
                onBottomReached()
            }
        }
    })
}
