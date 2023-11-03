package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Login
import com.github.libretube.api.obj.Token
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogLoginBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.preferences.InstanceSettings.Companion.INSTANCE_DIALOG_REQUEST_KEY
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLoginBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.login)
            .setPositiveButton(R.string.login, null)
            .setNegativeButton(R.string.register, null)
            .setView(binding.root)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val email = binding.username.text?.toString()
                    val password = binding.password.text?.toString()

                    if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                        signIn(email, password)
                    } else {
                        Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                    }
                }
                getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                    val email = binding.username.text?.toString().orEmpty()
                    val password = binding.password.text?.toString().orEmpty()

                    if (isEmail(email)) {
                        showPrivacyAlertDialog(email, password)
                    } else if (email.isNotEmpty() && password.isNotEmpty()) {
                        signIn(email, password, true)
                    } else {
                        Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun signIn(username: String, password: String, createNewAccount: Boolean = false) {
        val login = Login(username, password)
        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                if (createNewAccount) {
                    RetrofitInstance.authApi.register(login)
                } else {
                    RetrofitInstance.authApi.login(login)
                }
            } catch (e: HttpException) {
                val errorMessage = e.response()?.errorBody()?.string()?.runCatching {
                    JsonHelper.json.decodeFromString<Token>(this).error
                }?.getOrNull() ?: context?.getString(R.string.server_error).orEmpty()
                context?.toastFromMainDispatcher(errorMessage)
                return@launch
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                context?.toastFromMainDispatcher(e.localizedMessage.orEmpty())
                return@launch
            }

            if (response.error != null) {
                context?.toastFromMainDispatcher(response.error)
                return@launch
            }
            if (response.token == null) return@launch

            context?.toastFromMainDispatcher(
                if (createNewAccount) R.string.registered else R.string.loggedIn
            )

            PreferenceHelper.setToken(response.token)
            PreferenceHelper.setUsername(login.username)

            withContext(Dispatchers.Main) {
                setFragmentResult(
                    INSTANCE_DIALOG_REQUEST_KEY,
                    bundleOf(IntentData.loginTask to true)
                )
            }
            dialog?.dismiss()
        }
    }

    private fun showPrivacyAlertDialog(email: String, password: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_alert)
            .setMessage(R.string.username_email)
            .setNegativeButton(R.string.proceed) { _, _ ->
                signIn(email, password, true)
            }
            .setPositiveButton(R.string.cancel, null)
            .show()
    }

    private fun isEmail(text: String): Boolean {
        return Patterns.EMAIL_ADDRESS.toRegex().matches(text)
    }
}
