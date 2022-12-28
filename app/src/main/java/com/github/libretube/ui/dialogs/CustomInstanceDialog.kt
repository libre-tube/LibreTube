package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogCustomInstanceBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.extensions.query
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URL

class CustomInstanceDialog : DialogFragment() {
    private lateinit var binding: DialogCustomInstanceBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCustomInstanceBinding.inflate(layoutInflater)

        binding.cancel.setOnClickListener {
            dismiss()
        }

        binding.addInstance.setOnClickListener {
            val customInstance = CustomInstance(
                name = binding.instanceName.text.toString(),
                apiUrl = binding.instanceApiUrl.text.toString(),
                frontendUrl = binding.instanceFrontendUrl.text.toString()
            )

            if (
                customInstance.name != "" &&
                customInstance.apiUrl != "" &&
                customInstance.frontendUrl != ""
            ) {
                try {
                    // check whether the URL is valid, otherwise catch
                    URL(customInstance.apiUrl).toURI()
                    URL(customInstance.frontendUrl).toURI()

                    query {
                        Database.customInstanceDao().insertAll(customInstance)
                    }

                    activity?.recreate()
                    dismiss()
                } catch (e: Exception) {
                    // invalid URL
                    Toast.makeText(
                        context,
                        getString(R.string.invalid_url),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // at least one empty input
                Toast.makeText(
                    context,
                    context?.getString(R.string.empty_instance),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }
}
