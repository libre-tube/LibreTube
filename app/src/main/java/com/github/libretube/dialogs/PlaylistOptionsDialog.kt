package com.github.libretube.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.extensions.TAG
import com.github.libretube.obj.PlaylistId
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.toID
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
                                if (isOwner) RetrofitInstance.authApi.getPlaylist(playlistId)
                                else RetrofitInstance.api.getPlaylist(playlistId)
                            BackgroundHelper.playOnBackground(
                                context = requireContext(),
                                videoId = playlist.relatedStreams!![0].url.toID(),
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
                        val token = PreferenceHelper.getToken()
                        deletePlaylist(playlistId, token)
                    }
                }
            }
        return dialog.show()
    }

    private fun importPlaylist(token: String, playlistId: String) {
        fun run() {
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
        run()
    }

    private fun deletePlaylist(id: String, token: String) {
        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitInstance.authApi.deletePlaylist(token, PlaylistId(id))
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launch
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response")
                    return@launch
                }
            }
        }
        run()
    }
}
