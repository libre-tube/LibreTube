package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.obj.DeleteUserRequest
import com.github.libretube.requireMainActivityRestart
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteAccountDialog : DialogFragment() {
    private val TAG = "DeleteAccountDialog"
    lateinit var username: EditText
    lateinit var password: EditText
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_delete_account, null)

            view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog?.dismiss()
            }

            password = view.findViewById(R.id.delete_password)
            view.findViewById<Button>(R.id.delete_account_confirm).setOnClickListener {
                if (password.text.toString() != "") {
                    deleteAccount(password.text.toString())
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
            view.findViewById<TextView>(R.id.title).text = appName

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun deleteAccount(password: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                val token = sharedPref?.getString("token", "")!!

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
        val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", "")
        if (token != "") {
            with(sharedPref!!.edit()) {
                putString("token", "")
                apply()
            }
        }
    }
}
