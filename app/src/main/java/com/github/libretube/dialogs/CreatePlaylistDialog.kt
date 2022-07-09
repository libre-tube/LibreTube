package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.databinding.DialogCreatePlaylistBinding
import com.github.libretube.fragments.LibraryFragment
import com.github.libretube.obj.Playlists
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class CreatePlaylistDialog : DialogFragment() {
    val TAG = "CreatePlaylistDialog"
    private var token: String = ""
    private lateinit var binding: DialogCreatePlaylistBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

            binding.title.text = ThemeHelper.getStyledAppName(requireContext())

            binding.cancelButton.setOnClickListener {
                dismiss()
            }

            token = PreferenceHelper.getToken(requireContext())

            binding.createNewPlaylist.setOnClickListener {
                // avoid creating the same playlist multiple times by spamming the button
                binding.createNewPlaylist.setOnClickListener(null)
                val listName = binding.playlistName.text.toString()
                if (listName != "") {
                    createPlaylist(listName)
                } else {
                    Toast.makeText(context, R.string.emptyPlaylistName, Toast.LENGTH_LONG).show()
                }
            }

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun createPlaylist(name: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.authApi.createPlaylist(token, Playlists(name = name))
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response $e")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                if (response != null) {
                    Toast.makeText(context, R.string.playlistCreated, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_SHORT)
                        .show()
                }
                // refresh the playlists in the library
                try {
                    val parent = parentFragment as LibraryFragment
                    parent.fetchPlaylists()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
                dismiss()
            }
        }
        run()
    }
}
