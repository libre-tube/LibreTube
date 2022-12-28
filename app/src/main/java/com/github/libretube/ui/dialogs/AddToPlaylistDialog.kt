package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.DialogAddtoplaylistBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toStreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.ui.models.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddToPlaylistDialog(
    private val videoId: String
) : DialogFragment() {
    private lateinit var binding: DialogAddtoplaylistBinding
    private val viewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddtoplaylistBinding.inflate(layoutInflater)

        binding.createPlaylist.setOnClickListener {
            CreatePlaylistDialog {
                fetchPlaylists()
            }.show(childFragmentManager, null)
        }

        fetchPlaylists()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }

    private fun fetchPlaylists() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                PlaylistsHelper.getPlaylists()
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            if (response.isNotEmpty()) {
                val names = response.map { it.name }
                val arrayAdapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                arrayAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
                binding.playlistsSpinner.adapter = arrayAdapter
                if (viewModel.lastSelectedPlaylistId != null) {
                    var selectionIndex = 0
                    response.forEachIndexed { index, playlist ->
                        if (playlist.id == viewModel.lastSelectedPlaylistId) {
                            selectionIndex = index
                        }
                    }
                    binding.playlistsSpinner.setSelection(selectionIndex)
                }
                runOnUiThread {
                    binding.addToPlaylist.setOnClickListener {
                        val index = binding.playlistsSpinner.selectedItemPosition
                        viewModel.lastSelectedPlaylistId = response[index].id!!
                        addToPlaylist(response[index].id!!)
                        dialog?.dismiss()
                    }
                }
            }
        }
    }

    private fun addToPlaylist(playlistId: String) {
        val appContext = context?.applicationContext ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val success = try {
                PlaylistsHelper.addToPlaylist(
                    playlistId,
                    RetrofitInstance.api.getStreams(videoId).toStreamItem(videoId)
                )
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                appContext.toastFromMainThread(R.string.unknown_error)
                return@launch
            }
            appContext.toastFromMainThread(
                if (success) R.string.added_to_playlist else R.string.fail
            )
        }
    }

    private fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}
