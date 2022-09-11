package com.github.libretube.sheets

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.dialogs.ShareDialog
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.obj.PlaylistId
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException

class PlaylistOptionsDialog(
    private val playlistId: String,
    private val isOwner: Boolean
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // options for the dialog
        var optionsList = listOf(
            context?.getString(R.string.playOnBackground),
            context?.getString(R.string.clonePlaylist),
            context?.getString(R.string.share)
        )

        if (isOwner) {
            optionsList = optionsList +
                context?.getString(R.string.renamePlaylist)!! +
                context?.getString(R.string.deletePlaylist)!! -
                context?.getString(R.string.clonePlaylist)!!
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.video_options_dialog_item,
                    optionsList
                )
            ) { _, which ->
                when (optionsList[which]) {
                    // play the playlist in the background
                    context?.getString(R.string.playOnBackground) -> {
                        runBlocking {
                            val playlist =
                                if (isOwner) {
                                    RetrofitInstance.authApi.getPlaylist(playlistId)
                                } else {
                                    RetrofitInstance.api.getPlaylist(playlistId)
                                }
                            BackgroundHelper.playOnBackground(
                                context = requireContext(),
                                videoId = playlist.relatedStreams!![0].url!!.toID(),
                                playlistId = playlistId
                            )
                        }
                    }
                    // Clone the playlist to the users Piped account
                    context?.getString(R.string.clonePlaylist) -> {
                        val token = PreferenceHelper.getToken()
                        if (token != "") {
                            importPlaylist(token, playlistId)
                        } else {
                            Toast.makeText(
                                context,
                                R.string.login_first,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    // share the playlist
                    context?.getString(R.string.share) -> {
                        val shareDialog = ShareDialog(playlistId, true)
                        // using parentFragmentManager, childFragmentManager doesn't work here
                        shareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                    }
                    context?.getString(R.string.deletePlaylist) -> {
                        deletePlaylist(
                            playlistId
                        )
                    }
                    context?.getString(R.string.renamePlaylist) -> {
                        val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
                        binding.input.hint = context?.getString(R.string.playlistName)
                        binding.input.inputType = InputType.TYPE_CLASS_TEXT

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.renamePlaylist)
                            .setView(binding.root)
                            .setPositiveButton(R.string.okay) { _, _ ->
                                if (binding.input.text.toString() == "") {
                                    Toast.makeText(
                                        context,
                                        R.string.emptyPlaylistName,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setPositiveButton
                                }
                                renamePlaylist(playlistId, binding.input.text.toString())
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
            }
        return dialog.show()
    }

    private fun importPlaylist(token: String, playlistId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = try {
                RetrofitInstance.authApi.importPlaylist(token, PlaylistId(playlistId))
            } catch (e: IOException) {
                println(e)
                return@launch
            } catch (e: HttpException) {
                return@launch
            }
            Log.e(TAG(), response.toString())
        }
    }

    private fun renamePlaylist(id: String, newName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitInstance.authApi.renamePlaylist(
                    PreferenceHelper.getToken(),
                    PlaylistId(
                        playlistId = id,
                        newName = newName
                    )
                )
            } catch (e: Exception) {
                return@launch
            }
        }
    }

    private fun deletePlaylist(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitInstance.authApi.deletePlaylist(
                    PreferenceHelper.getToken(),
                    PlaylistId(id)
                )
            } catch (e: Exception) {
                return@launch
            }
        }
    }
}
