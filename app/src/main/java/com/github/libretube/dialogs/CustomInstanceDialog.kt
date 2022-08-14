package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.database.DatabaseHolder
import com.github.libretube.databinding.DialogCustomInstanceBinding
import com.github.libretube.obj.CustomInstance
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URL

class CustomInstanceDialog : DialogFragment() {
    private lateinit var binding: DialogCustomInstanceBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
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

                        Thread {
                            DatabaseHolder.database.customInstanceDao().insertAll(customInstance)
                        }.start()

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

            binding.title.text = ThemeHelper.getStyledAppName(requireContext())

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
