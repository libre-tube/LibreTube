package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.databinding.DialogDeleteAccountBinding
import com.github.libretube.obj.DeleteUserRequest
import com.github.libretube.requireMainActivityRestart
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteAccountDialog : DialogFragment() {
    private val TAG = "DeleteAccountDialog"
    private lateinit var binding: DialogDeleteAccountBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogDeleteAccountBinding.inflate(layoutInflater)

            binding.cancelButton.setOnClickListener {
                dialog?.dismiss()
            }

            binding.deleteAccountConfirm.setOnClickListener {
                if (binding.deletePassword.text.toString() != "") {
                    deleteAccount(binding.deletePassword.text.toString())
                } else {
                    Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                }
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

    private fun deleteAccount(password: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val token = PreferenceHelper.getToken(requireContext())

                try {
                    RetrofitInstance.api.deleteAccount(token, DeleteUserRequest(password))
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                requireMainActivityRestart = true
                Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                logout()
                dialog?.dismiss()
            }
        }
        run()
    }

    private fun logout() {
        PreferenceHelper.setToken(requireContext(), "")
    }
}
