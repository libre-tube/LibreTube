package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogCustomInstanceBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.extensions.query
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CustomInstanceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCustomInstanceBinding.inflate(layoutInflater)

        binding.cancel.setOnClickListener {
            dismiss()
        }

        binding.addInstance.setOnClickListener {
            val instanceName = binding.instanceName.text.toString()
            val apiUrl = binding.instanceApiUrl.text.toString()
            val frontendUrl = binding.instanceFrontendUrl.text.toString()

            if (instanceName.isNotEmpty() && apiUrl.isNotEmpty() && frontendUrl.isNotEmpty()) {
                if (apiUrl.toHttpUrlOrNull() != null && frontendUrl.toHttpUrlOrNull() != null) {
                    query {
                        Database.customInstanceDao()
                            .insertAll(CustomInstance(instanceName, apiUrl, frontendUrl))
                    }

                    ActivityCompat.recreate(requireActivity())
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), R.string.invalid_url, Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                // at least one empty input
                Toast.makeText(requireContext(), R.string.empty_instance, Toast.LENGTH_SHORT).show()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }
}
