package com.github.libretube.ui.extensions

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
fun PictureInPictureParams.Builder.setAspectRatio(
    width: Int,
    height: Int
): PictureInPictureParams.Builder {
    val ratio = (width.toFloat() / height).let {
        when {
            it.isNaN() -> Rational(4, 3)
            it <= 0.418410 -> Rational(41841, 100000)
            it >= 2.390000 -> Rational(239, 100)
            else -> Rational(width, height)
        }
    }
    return setAspectRatio(ratio)
}
