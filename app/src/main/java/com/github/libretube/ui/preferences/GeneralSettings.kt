package com.github.libretube.ui.preferences

import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.RequireRestartDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GeneralSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.general

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_settings, rootKey)

        val language = findPreference<ListPreference>("language")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            language?.setOnPreferenceChangeListener { _, _ ->
                RequireRestartDialog().show(
                    childFragmentManager,
                    RequireRestartDialog::class.java.name
                )
                true
            }
            val languages = requireContext().resources.getStringArray(R.array.languageCodes)
                .map { code ->
                    val locale = LocaleHelper.getLocaleFromAndroidCode(code)

                    // each language's name is displayed in its own language,
                    // e.g. 'de': 'Deutsch', 'fr': 'Francais', ...
                    locale.toString() to locale.getDisplayName(locale)
                }.sortedBy { it.second.lowercase() }
            language?.entries =
                arrayOf(requireContext().getString(R.string.systemLanguage)) + languages.map { it.second }
            language?.entryValues = arrayOf("sys") + languages.map { it.first }
        } else {
            // on newer Android versions, the language is set through Android settings
            // language is the only item in this category, so the whole category should be hidden
            language?.parent?.isVisible = false
        }

        val autoRotation = findPreference<ListPreference>(PreferenceKeys.ORIENTATION)
        autoRotation?.setOnPreferenceChangeListener { _, _ ->
            RequireRestartDialog().show(childFragmentManager, RequireRestartDialog::class.java.name)
            true
        }

        val maxImageCache = findPreference<ListPreference>(PreferenceKeys.MAX_IMAGE_CACHE)
        maxImageCache?.setOnPreferenceChangeListener { _, _ ->
            ImageHelper.initializeImageLoader(requireContext())
            true
        }

        val resetSettings = findPreference<Preference>(PreferenceKeys.RESET_SETTINGS)
        resetSettings?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset) { _, _ ->
                // clear default preferences
                PreferenceHelper.clearPreferences()

                // clear login token
                PreferenceHelper.setToken("")

                ActivityCompat.recreate(requireActivity())
            }
            .show()
    }
}
