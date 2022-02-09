package com.github.libretube

import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import okhttp3.HttpUrl
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val instance = findPreference<ListPreference>("instance")
        instance?.setOnPreferenceChangeListener { preference, newValue ->
            RetrofitInstance.url=newValue.toString()
            RetrofitInstance.lazyMgr.reset()
            true
        }
        val login = findPreference<Preference>("login_register")
        login?.setOnPreferenceClickListener {
            val newFragment = LoginDialog()
            newFragment.show(childFragmentManager,"fuck")
            true
        }

    }
}
