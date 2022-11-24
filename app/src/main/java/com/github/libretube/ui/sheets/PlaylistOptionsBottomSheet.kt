package com.github.libretube.ui.sheets

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.DeletePlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException

class PlaylistOptionsBottomSheet(
    private val playlistId: String,
    playlistName: String,
    private val playlistType: PlaylistType
) : BaseBottomSheet() {
    private val shareData = ShareData(currentPlaylist = playlistName)
    override fun onCreate(savedInstanceState: Bundle?) {
        // options for the dialog
        val optionsList = mutableListOf(
            context?.getString(R.string.playOnBackground)!!
        )

        if (playlistType == PlaylistType.PUBLIC) {
            optionsList.add(context?.getString(R.string.share)!!)
            optionsList.add(context?.getString(R.string.clonePlaylist)!!)
        } else {
            optionsList.add(context?.getString(R.string.renamePlaylist)!!)
            optionsList.add(context?.getString(R.string.deletePlaylist)!!)
        }

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // play the playlist in the background
                context?.getString(R.string.playOnBackground) -> {
                    runBlocking {
                        val playlist =
                            if (playlistType == PlaylistType.PRIVATE) {
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
                    val shareDialog = ShareDialog(playlistId, ShareObjectType.PLAYLIST, shareData)
                    // using parentFragmentManager, childFragmentManager doesn't work here
                    shareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                }
                context?.getString(R.string.deletePlaylist) -> {
                    DeletePlaylistDialog(playlistId, playlistType)
                        .show(parentFragmentManager, null)
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
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    PlaylistsHelper.renamePlaylist(playlistId, binding.input.text.toString())
                                } catch (e: Exception) {
                                    return@launch
                                }
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }
        super.onCreate(savedInstanceState)
    }

    private fun importPlaylist(token: String, playlistId: String) {
        val appContext = context?.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val response = try {
                RetrofitInstance.authApi.importPlaylist(
                    token,
                    PlaylistId(playlistId)
                )
            } catch (e: IOException) {
                println(e)
                return@launch
            } catch (e: HttpException) {
                return@launch
            }
            appContext?.toastFromMainThread(if (response.playlistId != null) R.string.playlistCloned else R.string.server_error)
        }
    }
}
