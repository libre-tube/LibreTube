package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.databinding.DialogCreatePlaylistBinding
import com.github.libretube.extensions.toastFromMainDispatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class CreatePlaylistDialog(
    private val onSuccess: () -> Unit = {},
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

        binding.clonePlaylist.setOnClickListener {
            val playlistUrl = binding.playlistUrl.text.toString().toHttpUrlOrNull()
            val appContext = context?.applicationContext

            playlistUrl?.queryParameter("list")?.let {
                lifecycleScope.launch {
                    requireDialog().hide()
                    val playlistId = withContext(Dispatchers.IO) {
                        runCatching {
                            PlaylistsHelper.clonePlaylist(it)
                        }.getOrNull()
                    }
                    if (playlistId != null) {
                        onSuccess()
                    }
                    appContext?.toastFromMainDispatcher(
                        if (playlistId != null) R.string.playlistCloned else R.string.server_error,
                    )
                    dismiss()
                }
            } ?: run {
                Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.createNewPlaylist.setOnClickListener {
            val appContext = context?.applicationContext
            val listName = binding.playlistName.text?.toString()
            if (!listName.isNullOrEmpty()) {
                // avoid creating the same playlist multiple times by spamming the button
                binding.createNewPlaylist.setOnClickListener(null)
                lifecycleScope.launch {
                    requireDialog().hide()
                    val playlistId = withContext(Dispatchers.IO) {
                        runCatching {
                            PlaylistsHelper.createPlaylist(listName)
                        }.getOrNull()
                    }
                    appContext?.toastFromMainDispatcher(
                        if (playlistId != null) R.string.playlistCreated else R.string.unknown_error,
                    )
                    playlistId?.let { onSuccess() }
                    dismiss()
                }
            } else {
                Toast.makeText(context, R.string.emptyPlaylistName, Toast.LENGTH_LONG).show()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .show()
    }
}
