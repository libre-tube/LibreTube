package com.github.libretube.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

fun Context.hideKeyboard(view: View) {
    getSystemService<InputMethodManager>()!!.hideSoftInputFromWindow(view.windowToken, 0)
}
