package com.github.libretube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.DialogAddToPlaylistBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.ui.models.PlaylistViewModel
import com.github.libretube.util.PlayingQueue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Dialog to insert new videos to a playlist
 * @param videoId The id of the video to add. If non is provided, insert the whole playing queue
 */
class AddToPlaylistDialog(
    private val videoId: String? = null
) : DialogFragment() {
    private val viewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddToPlaylistBinding.inflate(layoutInflater)

        binding.createPlaylist.setOnClickListener {
            CreatePlaylistDialog {
                fetchPlaylists(binding)
            }.show(childFragmentManager, null)
        }

        fetchPlaylists(binding)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun fetchPlaylists(binding: DialogAddToPlaylistBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    PlaylistsHelper.getPlaylists()
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                }

                val playlists = response.filter { !it.name.isNullOrEmpty() }
                if (playlists.isEmpty()) return@repeatOnLifecycle

                binding.playlistsSpinner.adapter =
                    ArrayAdapter(requireContext(), R.layout.dropdown_item, playlists.map { it.name!! })

                // select the last used playlist
                viewModel.lastSelectedPlaylistId?.let { id ->
                    val latestIndex = response.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: 0
                    binding.playlistsSpinner.setSelection(latestIndex)
                }
                binding.addToPlaylist.setOnClickListener {
                    val index = binding.playlistsSpinner.selectedItemPosition
                    val playlist = playlists[index]
                    viewModel.lastSelectedPlaylistId = playlist.id!!
                    dialog?.hide()
                    lifecycleScope.launch {
                        addToPlaylist(playlist.id, playlist.name!!)
                        dialog?.dismiss()
                    }
                }
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private suspend fun addToPlaylist(playlistId: String, playlistName: String) {
        val appContext = context?.applicationContext ?: return
        val streams = when {
            videoId != null -> listOfNotNull(
                runCatching {
                    RetrofitInstance.api.getStreams(videoId!!).toStreamItem(videoId)
                }.getOrNull()
            )

            else -> PlayingQueue.getStreams()
        }

        val success = try {
            if (streams.isEmpty()) throw IllegalArgumentException()
            PlaylistsHelper.addToPlaylist(playlistId, *streams.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG(), e.toString())
            appContext.toastFromMainDispatcher(R.string.unknown_error)
            return
        }
        if (success) {
            appContext.toastFromMainDispatcher(
                appContext.getString(R.string.added_to_playlist, playlistName)
            )
        } else {
            appContext.toastFromMainDispatcher(R.string.fail)
        }
    }
}
