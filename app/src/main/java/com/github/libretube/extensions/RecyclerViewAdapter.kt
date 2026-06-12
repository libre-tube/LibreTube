package com.github.libretube.extensions

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.helpers.ThemeHelper
import kotlin.math.absoluteValue

data class SwipeOptions(
    val clamp: Boolean = false,
    @field:DrawableRes val icon: Int? = null,
    val iconScale: Double = 1.0,
    @field:ColorInt val iconTintColor: Int? = com.google.android.material.R.attr.colorPrimaryFixed,
    @field:ColorInt val backgroundColor: Int? = androidx.appcompat.R.attr.colorPrimary,
    val getDisableSwipe: ((position: Int) -> Boolean)? = null,
    val onSwipeListener: (position: Int) -> Unit = {}
) {}

fun RecyclerView.setOnDismissListener(onDismissedListener: (position: Int) -> Unit) {
    setActionListener(
        swipeLeft = SwipeOptions(
            icon = R.drawable.ic_delete,
            iconTintColor = com.google.android.material.R.attr.colorOnError,
            backgroundColor = androidx.appcompat.R.attr.colorError,
            onSwipeListener = onDismissedListener
        )
    )
}

fun RecyclerView.setOnDraggedListener(onDragListener: (from: Int, to: Int) -> Unit) {
    setActionListener(
        onDragListener = onDragListener
    )
}

fun RecyclerView.setActionListener(
    swipeLeft: SwipeOptions? = null,
    swipeRight: SwipeOptions? = null,
    onDragListener: ((from: Int, to: Int) -> Unit)? = null,
    getDisableActions: ((position: Int) -> Boolean)? = null
) {
    var hasSwiped = false
    val itemTouchCallback =
        object : ItemTouchHelper.SimpleCallback(
            if (onDragListener != null) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0,
            when {
                swipeLeft != null && swipeRight != null -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                swipeLeft != null -> ItemTouchHelper.LEFT
                swipeRight != null -> ItemTouchHelper.RIGHT
                else -> 0
            }
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val position = viewHolder.absoluteAdapterPosition

                if (getDisableActions?.invoke(position) == true) {
                    return 0
                }

                var flags = super.getMovementFlags(recyclerView, viewHolder)
                if (swipeLeft?.getDisableSwipe?.let { it(position) } == true) {
                    flags = flags and ((ItemTouchHelper.LEFT shl 8).inv())
                }
                if (swipeRight?.getDisableSwipe?.let { it(position) } == true) {
                    flags = flags and ((ItemTouchHelper.RIGHT shl 8).inv())
                }

                return flags
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (onDragListener == null) return false

                onDragListener.invoke(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
                return true
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return when {
                    swipeLeft != null && swipeLeft.clamp -> Float.MAX_VALUE
                    swipeRight != null && swipeRight.clamp -> Float.MAX_VALUE
                    else -> super.getSwipeThreshold(viewHolder)
                }
            }

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                return when {
                    swipeLeft != null && swipeLeft.clamp -> Float.MAX_VALUE
                    swipeRight != null && swipeRight.clamp -> Float.MAX_VALUE
                    else -> super.getSwipeEscapeVelocity(defaultValue)
                }
            }

            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                return when {
                    swipeLeft != null && swipeLeft.clamp -> Float.MAX_VALUE
                    swipeRight != null && swipeRight.clamp -> Float.MAX_VALUE
                    else -> super.getSwipeVelocityThreshold(defaultValue)
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> swipeLeft?.onSwipeListener?.invoke(viewHolder.absoluteAdapterPosition)
                    ItemTouchHelper.RIGHT -> swipeRight?.onSwipeListener?.invoke(viewHolder.absoluteAdapterPosition)
                    else -> return
                }
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

                val direction = when {
                    dX < 0 -> ItemTouchHelper.LEFT
                    dX > 0 -> ItemTouchHelper.RIGHT
                    else -> {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false)
                        return
                    }
                }

                val swipe = when (direction) {
                    ItemTouchHelper.LEFT if swipeLeft != null -> swipeLeft
                    ItemTouchHelper.RIGHT if swipeRight != null -> swipeRight
                    else -> {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        return
                    }
                }

                val dX = if (swipe.clamp) {
                    val swipeThreshold = itemView.width * 0.3f
                    if (isCurrentlyActive && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        hasSwiped = false
                    }

                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && !isCurrentlyActive && dX.absoluteValue > swipeThreshold && !hasSwiped) {
                        hasSwiped = true
                        when (direction) {
                            ItemTouchHelper.LEFT -> swipeLeft?.onSwipeListener?.invoke(viewHolder.absoluteAdapterPosition)
                            ItemTouchHelper.RIGHT -> swipeRight?.onSwipeListener?.invoke(viewHolder.absoluteAdapterPosition)
                        }
                    }

                    val swipeClamp = itemView.width * 0.4f
                    val maxOvershoot = itemView.width * 0.1f
                    val clamped = dX.coerceIn(-swipeClamp, swipeClamp)
                    val overshoot = dX - clamped
                    val overshootFactor = (overshoot.absoluteValue / maxOvershoot).coerceIn(0f, 1f) * 0.2f
                    clamped + (overshoot * overshootFactor).coerceIn(-maxOvershoot, maxOvershoot)
                } else { dX }

                val itemViewEnd = when (direction) {
                    ItemTouchHelper.LEFT -> itemView.right
                    ItemTouchHelper.RIGHT -> itemView.left
                    else -> return
                }

                val itemViewEndWithOffset = itemViewEnd + dX.toInt()

                if (swipe.backgroundColor != null) {
                    // Draw the background
                    val background = ColorDrawable().apply {
                        color = ThemeHelper.getThemeColor(context, swipe.backgroundColor)
                        if (direction == ItemTouchHelper.LEFT) {
                            setBounds(itemViewEndWithOffset, itemView.top, itemView.right, itemView.bottom)
                        } else {
                            setBounds(itemView.left, itemView.top, itemViewEndWithOffset, itemView.bottom)
                        }
                    }
                    background.draw(c)
                }

                if (swipe.icon != null) {
                    val icon = ContextCompat.getDrawable(context, swipe.icon)!!.apply {
                        if (swipe.iconTintColor != null) {
                            setTint(ThemeHelper.getThemeColor(context, swipe.iconTintColor))
                        }
                    }

                    val intrinsicHeight = (icon.intrinsicHeight * swipe.iconScale).toInt()
                    val intrinsicWidth = (icon.intrinsicWidth * swipe.iconScale).toInt()

                    // Calculate position of the icon
                    val itemHeight = itemView.bottom - itemView.top
                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val iconMargin = (itemHeight - intrinsicHeight) / 4
                    val iconBottom = iconTop + intrinsicHeight

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            val iconLeft = itemViewEnd - iconMargin - intrinsicWidth
                            val iconRight = itemViewEnd - iconMargin

                            if (itemViewEndWithOffset < iconLeft) {
                                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                icon.draw(c)
                            }
                        }
                        ItemTouchHelper.RIGHT -> {
                            val iconLeft = itemViewEnd + iconMargin
                            val iconRight = itemViewEnd + iconMargin + intrinsicWidth

                            if (itemViewEndWithOffset > iconRight) {
                                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                                icon.draw(c)
                            }
                        }
                    }
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