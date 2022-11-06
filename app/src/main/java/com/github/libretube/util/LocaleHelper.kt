package com.github.libretube.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.obj.Country
import java.util.*

object LocaleHelper {

    fun updateLanguage(context: Context) {
        val languageName = PreferenceHelper.getString(PreferenceKeys.LANGUAGE, "sys")
        val locale = when {
            languageName == "sys" -> Locale.getDefault()
            languageName.contains("-") == true -> {
                val languageParts = languageName.split("-")
                Locale(
                    languageParts[0],
                    languageParts[1].replace("r", "")
                )
            }
            else -> Locale(languageName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) updateResources(context, locale)
        updateResourcesLegacy(context, locale)
    }

    private fun updateResources(context: Context, locale: Locale) {
        Locale.setDefault(locale)
        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale) {
        Locale.setDefault(locale)
        val resources: Resources = context.resources
        val configuration: Configuration = resources.getConfiguration()
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.getDisplayMetrics())
    }

    fun getDetectedCountry(context: Context, defaultCountryIsoCode: String): String {
        detectSIMCountry(context)?.let {
            if (it != "") return it
        }

        detectNetworkCountry(context)?.let {
            if (it != "") return it
        }

        detectLocaleCountry(context)?.let {
            if (it != "") return it
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
