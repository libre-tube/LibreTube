package com.github.libretube.extensions

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.setOnDismissListener(onDismissedListener: (position: Int) -> Unit) {
    setActionListener(
        swipeDirections = arrayOf(ItemTouchHelper.LEFT),
        onDismissedListener = onDismissedListener
    )
}

fun RecyclerView.setOnDraggedListener(onDragListener: (from: Int, to: Int) -> Unit) {
    setActionListener(
        dragDirections = arrayOf(ItemTouchHelper.DOWN, ItemTouchHelper.UP),
        onDragListener = onDragListener
    )
}

fun RecyclerView.setActionListener(
    swipeDirections: Array<Int> = arrayOf(),
    dragDirections: Array<Int> = arrayOf(),
    onDragListener: (from: Int, to: Int) -> Unit = { _, _ -> },
    onDismissedListener: (position: Int) -> Unit = {}
) {
    val itemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(
            dragDirections.fold(0) { a, b -> a or b },
            swipeDirections.fold(0) { a, b -> a or b }
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (dragDirections.isEmpty()) return false

                onDragListener.invoke(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onDismissedListener.invoke(viewHolder.absoluteAdapterPosition)
            }
        }

    ItemTouchHelper(itemTouchCallback).attachToRecyclerView(this)
}