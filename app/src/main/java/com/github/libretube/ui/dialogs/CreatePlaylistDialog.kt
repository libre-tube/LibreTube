package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.databinding.DialogCreatePlaylistBinding
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CreatePlaylistDialog(
    private val onSuccess: () -> Unit = {}
) : DialogFragment() {
    private lateinit var binding: DialogCreatePlaylistBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

        binding.clonePlaylist.setOnClickListener {
            val playlistUrl = binding.playlistUrl.text.toString()
            if (!TextUtils.validateUrl(playlistUrl)) {
                Toast.makeText(context, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PlaylistsHelper.clonePlaylist(requireContext().applicationContext, playlistUrl)
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.createNewPlaylist.setOnClickListener {
            // avoid creating the same playlist multiple times by spamming the button
            binding.createNewPlaylist.setOnClickListener(null)
            val listName = binding.playlistName.text.toString()
            if (listName != "") {
                lifecycleScope.launchWhenCreated {
                    PlaylistsHelper.createPlaylist(listName, requireContext().applicationContext)
                    onSuccess.invoke()
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
