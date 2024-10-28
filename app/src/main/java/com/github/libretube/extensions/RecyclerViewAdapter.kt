package com.github.libretube.extensions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.helpers.ThemeHelper

fun RecyclerView.setOnDismissListener(onDismissedListener: (position: Int) -> Unit) {
    setActionListener(
        allowSwipe = true,
        onDismissedListener = onDismissedListener
    )
}

fun RecyclerView.setOnDraggedListener(onDragListener: (from: Int, to: Int) -> Unit) {
    setActionListener(
        allowDrag = true,
        onDragListener = onDragListener
    )
}

fun RecyclerView.setActionListener(
    allowSwipe: Boolean = false,
    allowDrag: Boolean = false,
    onDragListener: (from: Int, to: Int) -> Unit = { _, _ -> },
    onDismissedListener: (position: Int) -> Unit = {}
) {
    val itemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(
            if (allowDrag) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0,
            if (allowSwipe) ItemTouchHelper.LEFT else 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (!allowDrag) return false

                onDragListener.invoke(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onDismissedListener.invoke(viewHolder.absoluteAdapterPosition)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                if (dX == 0f && !isCurrentlyActive) {
                    clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false)
                    return
                }

                // Get the background colors from the theme (should be a tone of red for most themes)
                val backgroundColor = ThemeHelper.getThemeColor(context, androidx.appcompat.R.attr.colorError)
                val onBackgroundColor = ThemeHelper.getThemeColor(context, com.google.android.material.R.attr.colorOnError)

                val itemViewEndWithOffset = itemView.right + dX.toInt()
                // Draw the red delete background
                val background = ColorDrawable().apply {
                    color = backgroundColor
                    setBounds(itemViewEndWithOffset, itemView.top, itemView.right, itemView.bottom)
                }
                background.draw(c)

                val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.apply {
                    setTint(onBackgroundColor)
                }

                // Calculate position of the delete icon
                val itemHeight = itemView.bottom - itemView.top
                val deleteIconTop = itemView.top + (itemHeight - deleteIcon.intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - deleteIcon.intrinsicHeight) / 2
                val deleteIconLeft = itemView.right - deleteIconMargin - deleteIcon.intrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + deleteIcon.intrinsicHeight

                // Draw the delete icon if it wouldn't overlap the recycler view item
                if (itemViewEndWithOffset < deleteIconLeft) {
                    deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                    deleteIcon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
                val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
                c?.drawRect(left, top, right, bottom, clearPaint)
            }
        }

    ItemTouchHelper(itemTouchCallback).attachToRecyclerView(this)
}