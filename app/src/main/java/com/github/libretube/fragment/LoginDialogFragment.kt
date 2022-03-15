package com.github.libretube.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.RetrofitInstance
import com.github.libretube.databinding.DialogLoginBinding
import com.github.libretube.databinding.DialogLogoutBinding
import com.github.libretube.model.UserWithPassword
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception

private const val TAG = "LoginDialog"

class LoginDialogFragment : DialogFragment() {

    private lateinit var dialogLoginBinding: DialogLoginBinding
    private lateinit var dialogLogoutBinding: DialogLogoutBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogLoginBinding = DialogLoginBinding.inflate(layoutInflater)
        dialogLogoutBinding = DialogLogoutBinding.inflate(layoutInflater)

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            var sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val token = sharedPref?.getString("token", "")
            Log.e("dafaq", token!!)

            if (token.isNotEmpty()) {
                dialogLogoutBinding.btnLogout.setOnClickListener {
                    Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
                    sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    with(sharedPref!!.edit()) {
                        putString("token", "")
                        apply()
                    }
                    dialog?.dismiss()
                }
            } else {
                dialogLoginBinding.btnLogin.setOnClickListener {
                    login(getUserWithPassword())
                }
                dialogLoginBinding.btnRegister.setOnClickListener {
                    register(getUserWithPassword())
                }
            }
            builder.setView(dialogLoginBinding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getUserWithPassword() =
        UserWithPassword(dialogLoginBinding.etUsername.text.toString(),
            dialogLoginBinding.etPassword.text.toString())

    private fun login(userWithPassword: UserWithPassword) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.login(userWithPassword)
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
                    Log.e(TAG, "dafaq? $e")
                    return@launchWhenCreated
                }
                if (response.error != null) {
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                } else if (response.token != null) {
                    Toast.makeText(context, R.string.loggedIn, Toast.LENGTH_SHORT).show()
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    with(sharedPref!!.edit()) {
                        putString("token", response.token)
                        apply()
                    }
                    dialog?.dismiss()
                }
            }
        }
        run()
    }

    private fun register(userWithPassword: UserWithPassword) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.register(userWithPassword)
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
                    Log.e(TAG, "dafaq? $e")
                    return@launchWhenCreated
                }
                if (response.error != null) {
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                } else if (response.token != null) {
                    Toast.makeText(context, R.string.registered, Toast.LENGTH_SHORT).show()
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    with(sharedPref!!.edit()) {
                        putString("token", response.token)
                        apply()
                    }
                    dialog?.dismiss()
                }
            }
        }
        run()
    }
}
