package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Playlists
import com.github.libretube.databinding.DialogCreatePlaylistBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.fragments.LibraryFragment
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class CreatePlaylistDialog : DialogFragment() {
    private var token: String = ""
    private lateinit var binding: DialogCreatePlaylistBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

        binding.title.text = ThemeHelper.getStyledAppName(requireContext())

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        token = PreferenceHelper.getToken()

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

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun createPlaylist(name: String) {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.authApi.createPlaylist(
                    token,
                    com.github.libretube.api.obj.Playlists(name = name)
                )
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response $e")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            if (response.playlistId != null) {
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
                Log.e(TAG(), e.toString())
            }
            dismiss()
        }
    }
}
