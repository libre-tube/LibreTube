package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.github.libretube.R
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
            val addInstanceButton = view.findViewById<Button>(R.id.addInstance)
            val cancelButton = view.findViewById<Button>(R.id.cancel)
            cancelButton.setOnClickListener {
                dismiss()
            }

            addInstanceButton.setOnClickListener {
                val instanceName = instanceNameEditText.text.toString()
                val instanceApiUrl = instanceApiUrlEditText.text.toString()

                if (instanceName != "" && instanceApiUrl != "") {
                    try {
                        // check whether the URL is valid, otherwise catch
                        val u = URL(instanceApiUrl).toURI()
                        saveCustomInstance(instanceName, instanceApiUrl)
                        activity?.recreate()
                        dismiss()
                    } catch (e: Exception) {
                        // invalid URL
                        Toast.makeText(
                            context, getString(R.string.invalid_url), Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // at least one empty input
                    Toast.makeText(
                        context, context?.getString(R.string.empty_instance), Toast.LENGTH_SHORT
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

    private fun saveCustomInstance(instanceName: String, instanceApiUrl: String) {
        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(requireContext())

        // get the names of the other custom instances
        var customInstancesNames = try {
            sharedPreferences
                .getStringSet("custom_instances_name", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        // get the urls of the other custom instances
        var customInstancesUrls = try {
            sharedPreferences
                .getStringSet("custom_instances_url", HashSet())!!.toList()
        } catch (e: Exception) {
            emptyList()
        }

        // append new instance to the list
        customInstancesNames += instanceName
        customInstancesUrls += instanceApiUrl
        Log.e(TAG, customInstancesNames.toString())

        // save them to the shared preferences
        sharedPreferences.edit()
            .putStringSet("custom_instances_name", HashSet(customInstancesNames))
            .putStringSet("custom_instances_url", HashSet(customInstancesUrls))
            .apply()
    }
}
