package com.github.libretube

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.obj.PlaylistId
import java.io.IOException
import retrofit2.HttpException

class AddtoPlaylistDialog : DialogFragment() {
    private val TAG = "AddToPlaylistDialog"
    private lateinit var videoId: String
    private lateinit var token: String
    private lateinit var spinner: Spinner
    private lateinit var button: Button
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            videoId = arguments?.getString("videoId")!!
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            token = sharedPref?.getString("token", "")!!
            var view: View = inflater.inflate(R.layout.dialog_addtoplaylist, null)
            spinner = view.findViewById(R.id.playlists_spinner)
            button = view.findViewById(R.id.addToPlaylist)
            if (token != "") {
                fetchPlaylists()
            }
            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
    private fun fetchPlaylists() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.playlists(token)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                if (response.isNotEmpty()) {
                    var names = emptyList<String>().toMutableList()
                    for (playlist in response) {
                        names.add(playlist.name!!)
                    }
                    val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = arrayAdapter
                    runOnUiThread {
                        button.setOnClickListener {
                            addToPlaylist(response[spinner.selectedItemPosition].id!!)
                        }
                    }
                } else {
                }
            }
        }
        run()
    }
    private fun addToPlaylist(playlistId: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.addToPlaylist(token, PlaylistId(playlistId, videoId))
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                if (response.message == "ok") {
                    Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                    dialog?.dismiss()
                } else {
                    Toast.makeText(context, R.string.fail, Toast.LENGTH_SHORT).show()
                }
            }
        }
        run()
    }
    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
