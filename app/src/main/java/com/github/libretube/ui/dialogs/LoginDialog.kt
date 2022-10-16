package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Login
import com.github.libretube.databinding.DialogLoginBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class LoginDialog : DialogFragment() {
    private lateinit var binding: DialogLoginBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogLoginBinding.inflate(layoutInflater)

        binding.login.setOnClickListener {
            if (isInsertionValid()) {
                signIn(
                    binding.username.text.toString(),
                    binding.password.text.toString()
                )
            } else {
                Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
            }
        }
        binding.register.setOnClickListener {
            if (isInsertionValid()) {
                signIn(
                    binding.username.text.toString(),
                    binding.password.text.toString(),
                    true
                )
            } else {
                Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
            }
        }

        binding.title.text = ThemeHelper.getStyledAppName(requireContext())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun isInsertionValid(): Boolean {
        return binding.username.text.toString() != "" && binding.password.text.toString() != ""
    }

    private fun signIn(username: String, password: String, createNewAccount: Boolean = false) {
        val login = Login(username, password)
        lifecycleScope.launchWhenCreated {
            val response = try {
                if (createNewAccount) {
                    RetrofitInstance.authApi.register(login)
                } else {
                    RetrofitInstance.authApi.login(login)
                }
            } catch (e: IOException) {
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: Exception) {
                return@launchWhenCreated
            }

            if (response.error != null) {
                Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            if (response.token == null) return@launchWhenCreated

            Toast.makeText(
                context,
                if (createNewAccount) R.string.registered else R.string.loggedIn,
                Toast.LENGTH_SHORT
            ).show()

            PreferenceHelper.setToken(response.token!!)
            PreferenceHelper.setUsername(login.username!!)

            activity?.recreate()
            dialog?.dismiss()
        }
    }

    private fun finish() {
        activity?.recreate()
        dialog?.dismiss()
    }
}
