package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.DeleteUserRequest
import com.github.libretube.databinding.DialogDeleteAccountBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteAccountDialog(
    private val onLogout: () -> Unit
) : DialogFragment() {
    private lateinit var binding: DialogDeleteAccountBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDeleteAccountBinding.inflate(layoutInflater)

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
        lifecycleScope.launchWhenCreated {
            val token = PreferenceHelper.getToken()

            try {
                RetrofitInstance.authApi.deleteAccount(token, DeleteUserRequest(password))
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()

            onLogout.invoke()
            dialog?.dismiss()
        }
    }
}
