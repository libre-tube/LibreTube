package com.github.libretube.util

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.Country
import java.util.*

object LocaleHelper {

    fun updateLanguage(context: Context) {
        val languageName = PreferenceHelper.getString(PreferenceKeys.LANGUAGE, "sys")
        if (languageName == "sys") {
            updateLocaleConf(context, Locale.getDefault())
        } else if (languageName.contains("-") == true) {
            val languageParts = languageName.split("-")
            val locale = Locale(
                languageParts[0],
                languageParts[1]
            )
            updateLocaleConf(context, locale)
        } else {
            val locale = Locale(languageName.toString())
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
        conf.setLocale(locale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(conf, dm)
    }

    fun getDetectedCountry(context: Context, defaultCountryIsoCode: String): String {
        detectSIMCountry(context)?.let {
            return it
        }

        detectNetworkCountry(context)?.let {
            return it
        }

        detectLocaleCountry(context)?.let {
            return it
        }

        return defaultCountryIsoCode
    }

    private fun detectSIMCountry(context: Context): String? {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.simCountryIso
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun detectNetworkCountry(context: Context): String? {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.networkCountryIso
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun detectLocaleCountry(context: Context): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return context.resources.configuration.locales[0].country
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getAvailableCountries(): List<Country> {
        val isoCountries = Locale.getISOCountries()
        val countries = mutableListOf<Country>()
        isoCountries.forEach { countryCode ->
            val locale = Locale("", countryCode)
            val countryName = locale.displayCountry
            countries.add(
                Country(
                    countryName,
                    countryCode
                )
            )
        }
        countries.sortBy { it.name }
        return countries
    }

    fun getAvailableLocales(): List<Country> {
        val availableLocales: Array<Locale> = Locale.getAvailableLocales()
        val locales = mutableListOf<Country>()

        availableLocales.forEach { locale ->
            if (locales.filter { it.code == locale.language }.isEmpty()) {
                locales.add(
                    Country(
                        locale.displayLanguage,
                        locale.language
                    )
                )
            }
        }
        return locales
    }
}
