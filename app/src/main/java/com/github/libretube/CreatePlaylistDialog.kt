package com.github.libretube

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.libretube.obj.Login

class CreatePlaylistDialog: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            val token = sharedPref?.getString("token","")
            var view: View
            Log.e("dafaq",token!!)
            if(token!=""){
                val sharedPref2 = context?.getSharedPreferences("username", Context.MODE_PRIVATE)
                val user = sharedPref2?.getString("username","")
                view = inflater.inflate(R.layout.dialog_logout, null)
                view.findViewById<TextView>(R.id.user).text = view.findViewById<TextView>(R.id.user).text.toString()+" ("+user+")"
                view.findViewById<Button>(R.id.logout).setOnClickListener {
                    Toast.makeText(context,R.string.loggedout, Toast.LENGTH_SHORT).show()
                    val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
                    with (sharedPref!!.edit()) {
                        putString("token","")
                        apply()
                    }
                    dialog?.dismiss()
                }
            }
            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}