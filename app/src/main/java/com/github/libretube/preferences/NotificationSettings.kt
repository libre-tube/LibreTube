package com.github.libretube.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.libretube.R
import com.github.libretube.activities.SettingsActivity
import com.github.libretube.util.NotificationHelper
import com.github.libretube.views.MaterialPreferenceFragment

class NotificationSettings : MaterialPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings, rootKey)

        val settingsActivity = activity as SettingsActivity
        settingsActivity.changeTopBarText(getString(R.string.notifications))

        val notificationsEnabled =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.NOTIFICATION_ENABLED)
        notificationsEnabled?.setOnPreferenceChangeListener { _, _ ->
            updateNotificationPrefs()
            true
        }

        val checkingFrequency = findPreference<ListPreference>(PreferenceKeys.CHECKING_FREQUENCY)
        checkingFrequency?.setOnPreferenceChangeListener { _, _ ->
            updateNotificationPrefs()
            true
        }
    }

    private fun updateNotificationPrefs() {
        // replace the previous queued work request
        NotificationHelper.enqueueWork(
            requireContext(),
            ExistingPeriodicWorkPolicy.REPLACE
        )
    }
}
