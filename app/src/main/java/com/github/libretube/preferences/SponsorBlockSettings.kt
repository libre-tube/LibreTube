package com.github.libretube.preferences

import android.os.Bundle
import android.widget.TextView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R

class SponsorBlockSettings : PreferenceFragmentCompat() {
    private val TAG = "SponsorBlockSettings"

    companion object {
        var sponsorBlockEnabled: Boolean = false
        var sponsorNotificationsEnabled: Boolean = false
        var sponsorsEnabled: Boolean = false
        var selfPromoEnabled: Boolean = false
        var interactionEnabled: Boolean = false
        var introEnabled: Boolean = false
        var outroEnabled: Boolean = false
        var fillerEnabled: Boolean = false
        var musicOfftopicEnabled: Boolean = false
        var previewEnabled: Boolean = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)

        val topBarTextView = activity?.findViewById<TextView>(R.id.topBar_textView)
        topBarTextView?.text = getString(R.string.sponsorblock)

        val sponsorBlockToggle = findPreference<SwitchPreferenceCompat>("sb_enabled_key")
        sponsorBlockToggle?.setOnPreferenceChangeListener { _, newValue ->
            sponsorBlockEnabled = newValue as Boolean
            true
        }

        val notificationsToggle = findPreference<SwitchPreferenceCompat>("sb_notifications_key")
        notificationsToggle?.setOnPreferenceChangeListener { _, newValue ->
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

        val fillerToggle = findPreference<SwitchPreferenceCompat>("filler_category_key")
        fillerToggle?.setOnPreferenceChangeListener { _, newValue ->
            fillerEnabled = newValue as Boolean
            true
        }

        val musicToggle = findPreference<SwitchPreferenceCompat>("music_offtopic_category_key")
        musicToggle?.setOnPreferenceChangeListener { _, newValue ->
            musicOfftopicEnabled = newValue as Boolean
            true
        }

        val previewToggle = findPreference<SwitchPreferenceCompat>("preview_category_key")
        previewToggle?.setOnPreferenceChangeListener { _, newValue ->
            previewEnabled = newValue as Boolean
            true
        }
    }
}
