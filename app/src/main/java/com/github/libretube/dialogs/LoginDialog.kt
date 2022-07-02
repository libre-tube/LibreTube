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
import com.github.libretube.activities.requireMainActivityRestart
import com.github.libretube.databinding.DialogLoginBinding
import com.github.libretube.obj.Login
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class LoginDialog : DialogFragment() {
    private val TAG = "LoginDialog"
    private lateinit var binding: DialogLoginBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            binding = DialogLoginBinding.inflate(layoutInflater)

            binding.login.setOnClickListener {
                if (binding.username.text.toString() != "" && binding.password.text.toString() != "") {
                    val login =
                        Login(binding.username.text.toString(), binding.password.text.toString())
                    login(login)
                } else {
                    Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                }
            }
            binding.register.setOnClickListener {
                if (
                    binding.username.text.toString() != "" &&
                    binding.password.text.toString() != ""
                ) {
                    val login = Login(
                        binding.username.text.toString(),
                        binding.password.text.toString()
                    )
                    register(login)
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

    private fun login(login: Login) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.login(login)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: Exception) {
                    Log.e(TAG, "dafaq?$e")
                    return@launchWhenCreated
                }
                if (response.error != null) {
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                } else if (response.token != null) {
                    Toast.makeText(context, R.string.loggedIn, Toast.LENGTH_SHORT).show()
                    PreferenceHelper.setToken(requireContext(), response.token!!)
                    PreferenceHelper.setUsername(requireContext(), login.username!!)
                    requireMainActivityRestart = true
                    dialog?.dismiss()
                }
            }
        }
        run()
    }

    private fun register(login: Login) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.register(login)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: Exception) {
                    Log.e(TAG, "dafaq?$e")
                    return@launchWhenCreated
                }
                if (response.error != null) {
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                } else if (response.token != null) {
                    Toast.makeText(context, R.string.registered, Toast.LENGTH_SHORT).show()
                    PreferenceHelper.setToken(requireContext(), response.token!!)
                    PreferenceHelper.setUsername(requireContext(), login.username!!)
                    dialog?.dismiss()
                }
            }
        }
        run()
    }
}
