package com.github.libretube.extensions

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat

inline fun <reified T : Parcelable> Intent.parcelableExtra(key: String?): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}
