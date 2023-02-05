package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.NotificationHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.views.TimePickerPreference

class NotificationSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.notifications

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_settings, rootKey)

        val notificationsEnabled =
            findPreference<SwitchPreferenceCompat>(PreferenceKeys.NOTIFICATION_ENABLED)
        val checkingFrequency = findPreference<ListPreference>(PreferenceKeys.CHECKING_FREQUENCY)
        val requiredNetwork = findPreference<ListPreference>(PreferenceKeys.REQUIRED_NETWORK)

        notificationsEnabled?.setOnPreferenceChangeListener { _, newValue ->
            checkingFrequency?.isEnabled = newValue as Boolean
            requiredNetwork?.isEnabled = newValue
            updateNotificationPrefs()
            true
        }

        checkingFrequency?.isEnabled = notificationsEnabled!!.isChecked
        checkingFrequency?.setOnPreferenceChangeListener { _, _ ->
            updateNotificationPrefs()
            true
        }

        requiredNetwork?.isEnabled = notificationsEnabled.isChecked
        requiredNetwork?.setOnPreferenceChangeListener { _, _ ->
            updateNotificationPrefs()
            true
        }

        val notificationTime = findPreference<SwitchPreferenceCompat>(
            PreferenceKeys.NOTIFICATION_TIME_ENABLED
        )
        val notificationStartTime = findPreference<TimePickerPreference>(
            PreferenceKeys.NOTIFICATION_START_TIME
        )
        val notificationEndTime = findPreference<TimePickerPreference>(
            PreferenceKeys.NOTIFICATION_END_TIME
        )
        listOf(notificationStartTime, notificationEndTime).forEach {
            it?.isEnabled = notificationTime?.isChecked == true
        }
        notificationTime?.setOnPreferenceChangeListener { _, newValue ->
            listOf(notificationStartTime, notificationEndTime).forEach {
                it?.isEnabled = newValue as Boolean
            }
            true
        }
    }

    private fun updateNotificationPrefs() {
        // replace the previous queued work request
        NotificationHelper
            .enqueueWork(
                context = requireContext(),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.REPLACE
            )
    }
}
