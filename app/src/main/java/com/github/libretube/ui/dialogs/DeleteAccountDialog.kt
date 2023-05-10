package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.DeleteUserRequest
import com.github.libretube.databinding.DialogDeleteAccountBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteAccountDialog(
    private val onLogout: () -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDeleteAccountBinding.inflate(layoutInflater)

        binding.cancelButton.setOnClickListener {
            dialog?.dismiss()
        }

        binding.deleteAccountConfirm.setOnClickListener {
            val password = binding.deletePassword.text?.toString()
            if (!password.isNullOrEmpty()) {
                deleteAccount(password)
            } else {
                Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun deleteAccount(password: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val token = PreferenceHelper.getToken()

                try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.authApi.deleteAccount(token, DeleteUserRequest(password))
                    }
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                }
                Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()

                onLogout.invoke()
                dialog?.dismiss()
            }
        }
    }
}
