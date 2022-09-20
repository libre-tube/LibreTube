package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogLogoutBinding
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogoutDialog : DialogFragment() {
    private lateinit var binding: DialogLogoutBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogLogoutBinding.inflate(layoutInflater)

        val user = PreferenceHelper.getUsername()

        binding.user.text =
            binding.user.text.toString() + " (" + user + ")"
        binding.logout.setOnClickListener {
            Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
            PreferenceHelper.setToken("")
            dialog?.dismiss()
            activity?.recreate()
        }

        binding.title.text = ThemeHelper.getStyledAppName(requireContext())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }
}
