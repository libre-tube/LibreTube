package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogDeleteAccountBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.repo.UserDataRepositoryHelper
import com.github.libretube.ui.preferences.InstanceSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteAccountDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDeleteAccountBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.deleteAccount)
            .setPositiveButton(R.string.deleteAccount, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    requireDialog().hide()

                    val password = binding.deletePassword.text?.toString()
                    if (password.isNullOrEmpty()) {
                        Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        deleteAccount(password)
                        dismiss()
                    }
                }
            }
    }

    private suspend fun deleteAccount(password: String) {
        try {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                UserDataRepositoryHelper.userDataRepository.deleteAccount(password)
            }
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()

        setFragmentResult(
            InstanceSettings.INSTANCE_DIALOG_REQUEST_KEY,
            bundleOf(IntentData.logoutTask to true)
        )
    }
}
