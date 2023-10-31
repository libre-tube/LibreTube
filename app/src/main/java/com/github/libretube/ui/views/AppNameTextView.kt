package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.util.TypedValueCompat
import com.github.libretube.helpers.ThemeHelper

class AppNameTextView : AppCompatTextView {
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    constructor(context: Context) : super(context, null)

    init {
        text = ThemeHelper.getStyledAppName(context)
        textSize = TypedValueCompat.spToPx(10f, resources.displayMetrics)
    }
}
