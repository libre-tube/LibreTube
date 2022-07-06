package com.github.libretube.util

import android.content.Context
import android.os.Build
import com.github.libretube.preferences.PreferenceHelper
import java.util.*

object LocaleHelper {

    fun updateLanguage(context: Context) {
        val languageName = PreferenceHelper.getString(context, "language", "sys")
        if (languageName == "sys") updateLocaleConf(context, Locale.getDefault())
        else if ("$languageName".length < 3) {
            val locale = Locale(languageName.toString())
            updateLocaleConf(context, locale)
        } else if ("$languageName".length > 3) {
            val locale = Locale(
                languageName?.substring(0, 2).toString(),
                languageName?.substring(4, 6).toString()
            )
            updateLocaleConf(context, locale)
        }
    }

    private fun updateLocaleConf(context: Context, locale: Locale) {
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
