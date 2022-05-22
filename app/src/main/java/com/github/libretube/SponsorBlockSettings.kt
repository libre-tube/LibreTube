package com.github.libretube

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SponsorBlockSettings : PreferenceFragmentCompat() {
    private val TAG = "SponsorBlockDialog"

    companion object {
        var sponsorBlockEnabled: Boolean = false
        var sponsorNotificationsEnabled: Boolean = false
        var sponsorsEnabled: Boolean = false
        var selfPromoEnabled: Boolean = false
        var interactionEnabled: Boolean = false
        var introEnabled: Boolean = false
        var outroEnabled: Boolean = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)
        val sponsorBlockToggle = findPreference<SwitchPreferenceCompat>("sponsorblock_enabled_key")
        sponsorBlockToggle?.setOnPreferenceChangeListener { _, newValue ->
            sponsorBlockEnabled = newValue as Boolean
            true
        }

        val sponsorBlockNotificationsToggle = findPreference<SwitchPreferenceCompat>("sponsorblock_notifications_key")
        sponsorBlockNotificationsToggle?.setOnPreferenceChangeListener { _, newValue ->
            sponsorNotificationsEnabled = newValue as Boolean
            true
        }

        val sponsorToggle = findPreference<SwitchPreferenceCompat>("sponsors_category_key")
        sponsorToggle?.setOnPreferenceChangeListener { _, newValue ->
            sponsorsEnabled = newValue as Boolean
            true
        }
        val selfPromoToggle = findPreference<SwitchPreferenceCompat>("selfpromo_category_key")
        selfPromoToggle?.setOnPreferenceChangeListener { _, newValue ->
            selfPromoEnabled = newValue as Boolean
            true
        }

        val interactionToggle = findPreference<SwitchPreferenceCompat>("interaction_category_key")
        interactionToggle?.setOnPreferenceChangeListener { _, newValue ->
            interactionEnabled = newValue as Boolean
            true
        }

        val introToggle = findPreference<SwitchPreferenceCompat>("intro_category_key")
        introToggle?.setOnPreferenceChangeListener { _, newValue ->
            introEnabled = newValue as Boolean
            true
        }

        val outroToggle = findPreference<SwitchPreferenceCompat>("outro_category_key")
        outroToggle?.setOnPreferenceChangeListener { _, newValue ->
            outroEnabled = newValue as Boolean
            true
        }
    }
}
