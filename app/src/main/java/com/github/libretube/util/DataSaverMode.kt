package com.github.libretube.util

import android.content.Context
import com.github.libretube.constants.PreferenceKeys

object DataSaverMode {
    fun isEnabled(context: Context): Boolean {
        val pref = PreferenceHelper.getString(PreferenceKeys.DATA_SAVER_MODE, "disabled")
        return when (pref) {
            "enabled" -> true
            "disabled" -> false
            "metered" -> NetworkHelper.isNetworkMobile(context)
            else -> throw IllegalArgumentException()
        }
    }
}
