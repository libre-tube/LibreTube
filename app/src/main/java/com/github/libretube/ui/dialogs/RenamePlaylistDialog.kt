package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainThread
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RenamePlaylistDialog(
    private val playlistId: String,
    private val currentPlaylistName: String
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
        binding.input.inputType = InputType.TYPE_CLASS_TEXT
        binding.input.hint = getString(R.string.playlistName)
        binding.input.setText(currentPlaylistName)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.renamePlaylist)
            .setView(binding.root)
            .setPositiveButton(R.string.okay) { _, _ ->
                val input = binding.input.text.toString()
                if (input == "") {
                    Toast.makeText(
                        context,
                        R.string.emptyPlaylistName,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (input == currentPlaylistName) return@setPositiveButton
                val appContext = requireContext().applicationContext
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = try {
                        PlaylistsHelper.renamePlaylist(playlistId, binding.input.text.toString())
                    } catch (e: Exception) {
                        Log.e(TAG(), e.toString())
                        e.localizedMessage?.let { appContext.toastFromMainThread(it) }
                        return@launch
                    }
                    if (success) {
                        appContext.toastFromMainThread(R.string.success)
                    } else {
                        appContext.toastFromMainThread(R.string.server_error)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
