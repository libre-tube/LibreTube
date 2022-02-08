package com.github.libretube

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.adapters.TrendingAdapter
import com.github.libretube.obj.Login
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception
import kotlin.math.log

class LoginDialog : DialogFragment() {
    private val TAG = "LoginDialog"
    lateinit var username: EditText
    lateinit var password: EditText
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.dialog_login, null)
            username=view.findViewById(R.id.username)
            password=view.findViewById(R.id.password)
            view.findViewById<Button>(R.id.login).setOnClickListener {
                val login = Login(username.text.toString(),password.text.toString())
                login(login)
            }
            view.findViewById<Button>(R.id.register).setOnClickListener {
                val login = Login(username.text.toString(),password.text.toString())
                register(login)
            }
            builder.setView(view)
                // Add action buttons
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
    private fun login(login: Login){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.login(login)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } catch (e: Exception) {
                    Log.e(TAG,"dafaq?"+e.toString())
                    return@launchWhenCreated
                }
                if (response.error!= null){
                    Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                }else if(response.token!=null){
                    Toast.makeText(context,R.string.loggedIn, Toast.LENGTH_SHORT).show()
                    dialog?.dismiss()
                }

            }
        }
        run()
    }
    private fun register(login: Login){
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.register(login)
                }catch(e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    return@launchWhenCreated
                } catch (e: Exception) {
                    Log.e(TAG,"dafaq?"+e.toString())
                    return@launchWhenCreated
                }
                if (response.error!= null){
                Toast.makeText(context, response.error, Toast.LENGTH_SHORT).show()
                }else if(response.token!=null){
                    Toast.makeText(context,R.string.registered, Toast.LENGTH_SHORT).show()
                    dialog?.dismiss()
                }

            }
        }
        run()
    }

}
