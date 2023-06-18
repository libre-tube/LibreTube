package com.github.libretube.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.parcelable(key: String?): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}
