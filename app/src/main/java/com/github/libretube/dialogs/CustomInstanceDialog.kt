package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class CustomInstanceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val view: View = inflater.inflate(R.layout.dialog_custom_instance, null)

            val instanceNameEditText = view.findViewById<TextInputEditText>(R.id.instanceName)
            val instanceApiUrlEditText = view.findViewById<TextInputEditText>(R.id.instanceApiUrl)
            val addInstanceButton = view.findViewById<Button>(R.id.addInstance)
            addInstanceButton.setOnClickListener {
                val instanceName = instanceNameEditText.text
                val instanceApiUrl = instanceApiUrlEditText.text
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