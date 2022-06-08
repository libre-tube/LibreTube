package com.github.libretube.util

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import java.util.*

class LocaleHelper {

    fun updateLanguage(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val languageName = sharedPreferences.getString("language", "sys")
        if (languageName != "") {
            setLanguage(context, languageName!!)
        }
    }

    private fun setLanguage(context: Context, languageName: String) {
        val locale = if (languageName != "sys" && "$languageName".length < 3) {
            Locale(languageName)
        } else if ("$languageName".length > 3) {
            Locale(languageName?.substring(0, 2), languageName?.substring(4, 6))
        } else {
            Locale.getDefault()
        }
        // Change API Language
        Locale.setDefault(locale)

        // Change App Language
        val res = context.resources
        val dm = res.displayMetrics
        val conf = res.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            conf.setLocale(locale)
        } else {
            conf.locale = locale
        }
        res.updateConfiguration(conf, dm)
    }
}
