package com.github.libretube.obj.bottomsheetitem

import android.content.Context
import androidx.annotation.StringRes

sealed class BottomSheetItemTitle {
    abstract fun getText(context: Context): String

    data class Text(private val text: String) : BottomSheetItemTitle() {
        override fun getText(context: Context): String = text
    }

    data class StringResource(
        @get:StringRes
        private val stringRes: Int
    ) : BottomSheetItemTitle() {
        override fun getText(context: Context): String = with(context) { getString(stringRes) }
    }

    data class StringResourceWithArgs(
        @get:StringRes val formatStringRes: Int,
        val args: List<Any>
    ) : BottomSheetItemTitle() {
        override fun getText(context: Context): String =
            with(context) { getString(formatStringRes, *args.toTypedArray()) }
    }
}