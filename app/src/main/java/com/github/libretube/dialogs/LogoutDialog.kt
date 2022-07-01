package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogLogoutBinding
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogoutDialog : DialogFragment() {
    private val TAG = "LogoutDialog"
    private lateinit var binding: DialogLogoutBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogLogoutBinding.inflate(layoutInflater)

            val user = PreferenceHelper.getUsername(requireContext())

            binding.user.text =
                binding.user.text.toString() + " (" + user + ")"
            binding.logout.setOnClickListener {
                Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
                PreferenceHelper.setToken(requireContext(), "")
                dialog?.dismiss()
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
