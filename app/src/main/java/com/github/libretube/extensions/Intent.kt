package com.github.libretube.extensions

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.core.content.IntentCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String?): T? {
    return IntentCompat.getParcelableExtra(this, name, T::class.java)
}

inline fun <reified T : Serializable> Intent.serializableExtra(name: String?): T? {
    return extras?.serializable(name)
}
