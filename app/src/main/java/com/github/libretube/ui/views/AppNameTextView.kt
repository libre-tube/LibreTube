package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import com.github.libretube.helpers.ThemeHelper

class AppNameTextView : AppCompatTextView {
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    constructor(context: Context) : super(context, null)

    init {
        text = ThemeHelper.getStyledAppName(context)
        textSize = spToPixel(10f)
    }

    @Suppress("SameParameterValue")
    private fun spToPixel(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
