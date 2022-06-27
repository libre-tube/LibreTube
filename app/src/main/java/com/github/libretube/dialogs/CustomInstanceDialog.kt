package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.obj.CustomInstance
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.net.URL

class CustomInstanceDialog : DialogFragment() {
    val TAG = "CustomInstanceDialog"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val view: View = inflater.inflate(R.layout.dialog_custom_instance, null)

            val instanceNameEditText = view.findViewById<TextInputEditText>(R.id.instanceName)
            val instanceApiUrlEditText = view.findViewById<TextInputEditText>(R.id.instanceApiUrl)
            val instanceFrontendUrlEditText = view
                .findViewById<TextInputEditText>(R.id.instanceFrontendUrl)

            val addInstanceButton = view.findViewById<Button>(R.id.addInstance)
            val cancelButton = view.findViewById<Button>(R.id.cancel)
            cancelButton.setOnClickListener {
                dismiss()
            }

            addInstanceButton.setOnClickListener {
                val customInstance = CustomInstance()
                customInstance.name = instanceNameEditText.text.toString()
                customInstance.apiUrl = instanceApiUrlEditText.text.toString()
                customInstance.frontendUrl = instanceFrontendUrlEditText.text.toString()

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
            view.findViewById<TextView>(R.id.title).text = appName

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
