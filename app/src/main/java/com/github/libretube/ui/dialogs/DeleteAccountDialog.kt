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
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.DeleteUserRequest
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogDeleteAccountBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.PreferenceHelper
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

    /**
     * Deletes the user account from the Piped instance.
     * @param password The user's password for verification
     */
    private suspend fun deleteAccount(password: String) {
        val token = PreferenceHelper.getToken()
        
        if (token.isEmpty()) {
            Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            withContext(Dispatchers.IO) {
                RetrofitInstance.authApi.deleteAccount(token, DeleteUserRequest(password))
            }
            
            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()

            setFragmentResult(
                InstanceSettings.INSTANCE_DIALOG_REQUEST_KEY,
                bundleOf(IntentData.logoutTask to true)
            )
        } catch (e: Exception) {
            Log.e(TAG(), "Account deletion failed: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("401") == true -> R.string.invalid_password
                e.message?.contains("403") == true -> R.string.unauthorized
                else -> R.string.unknown_error
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
