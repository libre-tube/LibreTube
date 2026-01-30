package com.github.libretube.ui.base

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.libretube.R
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.ui.extensions.onSystemInsets
import com.github.libretube.ui.preferences.EditNumberPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * PreferenceFragmentCompat using the [MaterialAlertDialogBuilder] instead of the old dialog builder
 */
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    /**
     * Whether any preference dialog is currently visible to the user.
     */
    var isDialogVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // add bottom padding to the list, to ensure that the last item is not overlapped by the system bars
        listView.onSystemInsets { v, systemInsets ->
            v.updatePadding(bottom = v.paddingBottom + systemInsets.bottom)
        }
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
                    .setPositiveButton(R.string.okay, null)
                    .setOnDismissListener { isDialogVisible = false }
                    .show()
            }

            is EditTextPreference -> {
                val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
                binding.input.setText(preference.text)

                if (preference is EditNumberPreference) {
                    binding.input.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setView(binding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val newValue = binding.input.text.toString()
                        if (preference is EditNumberPreference && newValue.toIntOrNull() == null) {
                            Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }

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
