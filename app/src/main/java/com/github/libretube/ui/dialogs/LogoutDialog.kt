package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogLogoutBinding
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogoutDialog(
    private val onLogout: () -> Unit
) : DialogFragment() {
    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogoutBinding.inflate(layoutInflater)

        val user = PreferenceHelper.getUsername()

        binding.user.text =
            binding.user.text.toString() + " (" + user + ")"
        binding.logout.setOnClickListener {
            Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()

            onLogout.invoke()
            dialog?.dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }
}
