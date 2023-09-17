package com.github.libretube.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import java.io.Serializable
import kotlin.reflect.safeCast

inline fun <reified T : Parcelable> Bundle.parcelable(key: String?): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}

inline fun <reified T : Serializable> Bundle.serializable(key: String?): T? {
    return getSerializable(this, key, T::class.java)
}

fun <T : Serializable> getSerializable(bundle: Bundle, key: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        bundle.getSerializable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        clazz.kotlin.safeCast(bundle.getSerializable(key))
    }
}
