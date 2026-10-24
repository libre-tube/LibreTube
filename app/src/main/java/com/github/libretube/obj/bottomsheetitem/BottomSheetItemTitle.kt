package com.github.libretube.obj.bottomsheetitem

import android.content.Context
import androidx.annotation.StringRes

sealed class BottomSheetItemTitle {
    abstract fun getText(context: Context): String

    data class Text(private val text: String) : BottomSheetItemTitle() {
        override fun getText(context: Context): String = text
    }

    data class StringResource(
        @get:StringRes @field:StringRes
        private val stringRes: Int,
        val args: List<Any> = listOf(),
    ) : BottomSheetItemTitle() {
        override fun getText(context: Context): String = context.getString(stringRes, *args.toTypedArray())
    }

}