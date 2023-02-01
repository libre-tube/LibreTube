package com.github.libretube.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.github.libretube.R

/**
 * TextView with custom sizable drawable support.
 */
class DrawableTextView(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    private var drawableStartDimen = 0F
    private var drawableTopDimen = 0F
    private var drawableEndDimen = 0F
    private var drawableBottomDimen = 0F

    init {
        val ta = getContext().obtainStyledAttributes(attrs, R.styleable.DrawableTextView, 0, 0)
        try {
            drawableStartDimen = getDimen(ta, R.styleable.DrawableTextView_drawableStartDimen)
            drawableTopDimen = getDimen(ta, R.styleable.DrawableTextView_drawableTopDimen)
            drawableEndDimen = getDimen(ta, R.styleable.DrawableTextView_drawableEndDimen)
            drawableBottomDimen = getDimen(ta, R.styleable.DrawableTextView_drawableBottomDimen)

            gravity = ta.getInt(
                R.styleable.DrawableTextView_android_gravity, Gravity.CENTER_VERTICAL
            )
            compoundDrawablePadding = ta.getDimensionPixelOffset(
                R.styleable.DrawableTextView_android_drawablePadding, 20
            )
        } finally {
            ta.recycle()
        }
    }

    private fun getDimen(ta: TypedArray, index: Int): Float {
        return ta.getDimensionPixelOffset(index, 0).toFloat()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val relDrawables = adjust(compoundDrawablesRelative)
        setCompoundDrawablesRelative(
            relDrawables[0],
            relDrawables[1],
            relDrawables[2],
            relDrawables[3]
        )
    }

    private fun adjust(drawables: Array<Drawable?>): Array<Drawable?> {
        val startDimen = drawableStartDimen.toInt()
        val start = drawables[0]
        if (startDimen > 0 && start != null) {
            start.setBounds(0, 0, startDimen, startDimen)
            drawables[0] = start
        }
        val tDimen = drawableTopDimen.toInt()
        val top = drawables[1]
        if (tDimen > 0 && top != null) {
            top.setBounds(0, 0, tDimen, tDimen)
            drawables[1] = top
        }
        val endDimen = drawableEndDimen.toInt()
        val end = drawables[2]
        if (endDimen > 0 && end != null) {
            end.setBounds(0, 0, endDimen, endDimen)
            drawables[2] = end
        }
        val bDimen = drawableBottomDimen.toInt()
        val bottom = drawables[3]
        if (bDimen > 0 && bottom != null) {
            bottom.setBounds(0, 0, bDimen, bDimen)
            drawables[3] = bottom
        }
        return drawables
    }

    fun setDrawables(
        start: Drawable? = null,
        top: Drawable? = null,
        end: Drawable? = null,
        bottom: Drawable? = null
    ) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
            this,
            start,
            top,
            end,
            bottom
        )
    }

    fun setDrawableStartDimension(
        start: Float = drawableStartDimen,
        top: Float = drawableTopDimen,
        end: Float = drawableEndDimen,
        bottom: Float = drawableBottomDimen
    ) {
        drawableStartDimen = start
        drawableTopDimen = top
        drawableEndDimen = end
        drawableBottomDimen = bottom
    }
}