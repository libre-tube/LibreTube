package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.databinding.DialogAddtoplaylistBinding
import com.github.libretube.obj.PlaylistId
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class AddtoPlaylistDialog : DialogFragment() {
    private val TAG = "AddToPlaylistDialog"
    private lateinit var binding: DialogAddtoplaylistBinding

    private lateinit var videoId: String
    private lateinit var token: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            videoId = arguments?.getString("videoId")!!
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            binding = DialogAddtoplaylistBinding.inflate(layoutInflater)

            token = PreferenceHelper.getToken(requireContext())

            if (token != "") fetchPlaylists()

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

    private fun fetchPlaylists() {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.authApi.playlists(token)
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
                    val arrayAdapter =
                        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                    arrayAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )
                    binding.playlistsSpinner.adapter = arrayAdapter
                    runOnUiThread {
                        binding.addToPlaylist.setOnClickListener {
                            addToPlaylist(
                                response[binding.playlistsSpinner.selectedItemPosition].id!!
                            )
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
                    RetrofitInstance.authApi.addToPlaylist(token, PlaylistId(playlistId, videoId))
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
