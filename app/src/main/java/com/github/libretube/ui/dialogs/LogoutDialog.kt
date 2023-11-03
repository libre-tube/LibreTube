package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogLogoutBinding
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.preferences.InstanceSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LogoutDialog : DialogFragment() {
    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogoutBinding.inflate(layoutInflater)

        val user = PreferenceHelper.getUsername()

        binding.user.text = binding.user.text.toString() + " ($user)"
        binding.logout.setOnClickListener {
            Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()

            setFragmentResult(
                InstanceSettings.INSTANCE_DIALOG_REQUEST_KEY,
                bundleOf(IntentData.logoutTask to true)
            )
            dialog?.dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setView(binding.root)
            .show()
    }
}
