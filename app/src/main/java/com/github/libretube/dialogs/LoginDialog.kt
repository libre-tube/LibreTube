package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.obj.Login
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import retrofit2.HttpException

class LoginDialog : DialogFragment() {
    private val TAG = "LoginDialog"
    lateinit var username: EditText
    lateinit var password: EditText
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val token = sharedPref?.getString("token", "")
            var view: View
            Log.e("dafaq", token!!)
            if (token != "") {
                val sharedPref2 = context?.getSharedPreferences("username", Context.MODE_PRIVATE)
                val user = sharedPref2?.getString("username", "")
                view = inflater.inflate(R.layout.dialog_logout, null)
                view.findViewById<TextView>(R.id.user).text =
                    view.findViewById<TextView>(R.id.user).text.toString() + " (" + user + ")"
                view.findViewById<Button>(R.id.logout).setOnClickListener {
                    Toast.makeText(context, R.string.loggedout, Toast.LENGTH_SHORT).show()
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    with(sharedPref!!.edit()) {
                        putString("token", "")
                        apply()
                    }
                    dialog?.dismiss()
                }
            } else {
                view = inflater.inflate(R.layout.dialog_login, null)
                username = view.findViewById(R.id.username)
                password = view.findViewById(R.id.password)
                view.findViewById<Button>(R.id.login).setOnClickListener {
                    if (username.text.toString() != "" && password.text.toString() != "") {
                        val login = Login(username.text.toString(), password.text.toString())
                        login(login)
                    } else {
                        Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                    }
                }
                view.findViewById<Button>(R.id.register).setOnClickListener {
                    if (username.text.toString() != "" && password.text.toString() != "") {
                        val login = Login(username.text.toString(), password.text.toString())
                        register(login)
                    } else {
                        Toast.makeText(context, R.string.empty, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val typedValue = TypedValue()
            this.requireActivity().theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            view.findViewById<TextView>(R.id.title).text = appName

            builder.setView(view)
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
                    Log.e(TAG, "dafaq?" + e.toString())
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
                    val sharedPref2 =
                        context?.getSharedPreferences("username", Context.MODE_PRIVATE)
                    with(sharedPref2!!.edit()) {
                        putString("username", login.username)
                        apply()
                    }
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
                    Log.e(TAG, "dafaq?" + e.toString())
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
                    val sharedPref2 =
                        context?.getSharedPreferences("username", Context.MODE_PRIVATE)
                    with(sharedPref2!!.edit()) {
                        putString("username", login.username)
                        apply()
                    }
                    dialog?.dismiss()
                }
            }
        }
        run()
    }
}
