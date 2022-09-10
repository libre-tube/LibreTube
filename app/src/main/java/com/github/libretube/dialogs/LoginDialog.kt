package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.DialogLoginBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.Login
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

        binding.title.text = ThemeHelper.getStyledAppName(requireContext())

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun login(login: Login) {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.authApi.login(login)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: Exception) {
                Log.e(TAG(), "dafaq?$e")
                return@launchWhenCreated
            }
            if (response.error != null) {
                Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
            } else if (response.token != null) {
                Toast.makeText(context, R.string.loggedIn, Toast.LENGTH_SHORT).show()
                PreferenceHelper.setToken(response.token!!)
                PreferenceHelper.setUsername(login.username!!)
                dialog?.dismiss()
                activity?.recreate()
            }
        }
    }

    private fun register(login: Login) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.authApi.register(login)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: Exception) {
                    Log.e(TAG(), "dafaq?$e")
                    return@launchWhenCreated
                }
                if (response.error != null) {
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                } else if (response.token != null) {
                    Toast.makeText(context, R.string.registered, Toast.LENGTH_SHORT).show()
                    PreferenceHelper.setToken(response.token!!)
                    PreferenceHelper.setUsername(login.username!!)
                    dialog?.dismiss()
                }
            }
        }
        run()
    }
}
