package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogCustomInstanceBinding
import com.github.libretube.obj.CustomInstance
import com.github.libretube.preferences.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URL

class CustomInstanceDialog : DialogFragment() {
    val TAG = "CustomInstanceDialog"
    private lateinit var binding: DialogCustomInstanceBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogCustomInstanceBinding.inflate(layoutInflater)

            binding.cancel.setOnClickListener {
                dismiss()
            }

            binding.addInstance.setOnClickListener {
                val customInstance = CustomInstance()
                customInstance.name = binding.instanceName.text.toString()
                customInstance.apiUrl = binding.instanceApiUrl.text.toString()
                customInstance.frontendUrl = binding.instanceFrontendUrl.text.toString()

                if (
                    customInstance.name != "" &&
                    customInstance.apiUrl != "" &&
                    customInstance.frontendUrl != ""
                ) {
                    try {
                        // check whether the URL is valid, otherwise catch
                        URL(customInstance.apiUrl).toURI()
                        URL(customInstance.frontendUrl).toURI()

                        PreferenceHelper.saveCustomInstance(requireContext(), customInstance)
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

            val typedValue = TypedValue()
            this.requireActivity().theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            binding.title.text = appName

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
