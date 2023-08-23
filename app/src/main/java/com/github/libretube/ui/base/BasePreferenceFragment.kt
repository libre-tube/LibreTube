package com.github.libretube.ui.base

import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.ui.activities.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * PreferenceFragmentCompat using the [MaterialAlertDialogBuilder] instead of the old dialog builder
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    abstract val titleResourceId: Int

    /**
     * Whether any preference dialog is currently visible to the user.
     */
    var isDialogVisible = false

    override fun onStart() {
        super.onStart()
        (activity as? SettingsActivity)?.changeTopBarText(getString(titleResourceId))
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        // can be set to true here since we only use the following preferences with dialogs
        isDialogVisible = true

        when (preference) {
            /**
             * Show a [MaterialAlertDialogBuilder] when the preference is a [ListPreference]
             */
            is ListPreference -> {
                // get the index of the previous selected item
                val prefIndex = preference.entryValues.indexOf(preference.value)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setSingleChoiceItems(preference.entries, prefIndex) { dialog, index ->
                        // get the new ListPreference value
                        val newValue = preference.entryValues[index].toString()
                        // invoke the on change listeners
                        if (preference.callChangeListener(newValue)) {
                            preference.value = newValue
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener { isDialogVisible = false }
                    .show()
            }

            is MultiSelectListPreference -> {
                val selectedItems = preference.entryValues.map {
                    preference.values.contains(it)
                }.toBooleanArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setMultiChoiceItems(preference.entries, selectedItems) { _, _, _ ->
                        val newValues = preference.entryValues
                            .filterIndexed { index, _ -> selectedItems[index] }
                            .map { it.toString() }
                            .toMutableSet()
                        if (preference.callChangeListener(newValues)) {
                            preference.values = newValues
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener { isDialogVisible = false }
                    .show()
            }

            is EditTextPreference -> {
                val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
                binding.input.setText(preference.text)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setView(binding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val newValue = binding.input.text.toString()
                        if (preference.callChangeListener(newValue)) {
                            preference.text = newValue
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener { isDialogVisible = false }
                    .show()
            }
            /**
             * Otherwise show the normal dialog, dialogs for other preference types are not supported yet,
             * nor used anywhere inside the app
             */
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }
}
