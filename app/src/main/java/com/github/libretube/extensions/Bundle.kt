package com.github.libretube.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.core.os.BuildCompat
import androidx.core.os.BundleCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Bundle.parcelable(key: String?): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}

@OptIn(BuildCompat.PrereleaseSdkCheck::class)
inline fun <reified T : Serializable> Bundle.serializable(key: String): T? {
    return if (BuildCompat.isAtLeastU()) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}
