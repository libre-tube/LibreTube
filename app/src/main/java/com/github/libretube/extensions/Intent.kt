package com.github.libretube.extensions

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.core.os.BuildCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String?): T? {
    return IntentCompat.getParcelableExtra(this, name, T::class.java)
}

@OptIn(BuildCompat.PrereleaseSdkCheck::class)
inline fun <reified T : Serializable> Intent.serializableExtra(name: String?): T? {
    return if (BuildCompat.isAtLeastU()) {
        getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(name) as? T
    }
}
