package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
        lifecycleScope.launchWhenCreated {
            val response = try {
                PlaylistsHelper.getPlaylists()
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            if (response.isEmpty()) return@launchWhenCreated
            val names = response.mapNotNull { it.name }
            val arrayAdapter =
                ArrayAdapter(requireContext(), R.layout.dropdown_item, names)
            binding.playlistsSpinner.adapter = arrayAdapter

            // select the last used playlist
            viewModel.lastSelectedPlaylistId?.let { id ->
                binding.playlistsSpinner.setSelection(
                    response.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: 0
                )
            }
            binding.addToPlaylist.setOnClickListener {
                val index = binding.playlistsSpinner.selectedItemPosition
                viewModel.lastSelectedPlaylistId = response[index].id!!
                dialog?.hide()
                lifecycleScope.launch {
                    addToPlaylist(response[index].id!!)
                    dialog?.dismiss()
                }
            }
        }
    }

    private suspend fun addToPlaylist(playlistId: String) {
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
        appContext.toastFromMainDispatcher(
            if (success) R.string.added_to_playlist else R.string.fail
        )
    }
}
