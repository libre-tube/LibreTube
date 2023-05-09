package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
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
import com.github.libretube.extensions.toastFromMainDispatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePlaylistDialog(
    private val playlistId: String,
    private val currentPlaylistName: String,
    private val onSuccess: (String) -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
        binding.input.inputType = InputType.TYPE_CLASS_TEXT
        binding.input.hint = getString(R.string.playlistName)
        binding.input.setText(currentPlaylistName)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.renamePlaylist)
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val newPlaylistName = binding.input.text?.toString()
                    if (newPlaylistName.isNullOrEmpty()) {
                        Toast.makeText(context, R.string.emptyPlaylistName, Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    if (newPlaylistName == currentPlaylistName) {
                        dismiss()
                        return@setOnClickListener
                    }
                    val appContext = requireContext().applicationContext

                    lifecycleScope.launch {
                        requireDialog().hide()
                        val success = try {
                            withContext(Dispatchers.IO) {
                                PlaylistsHelper.renamePlaylist(playlistId, newPlaylistName)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG(), e.toString())
                            e.localizedMessage?.let { appContext.toastFromMainDispatcher(it) }
                            return@launch
                        }
                        if (success) {
                            appContext.toastFromMainDispatcher(R.string.success)
                            onSuccess.invoke(newPlaylistName)
                        } else {
                            appContext.toastFromMainDispatcher(R.string.server_error)
                        }
                        dismiss()
                    }
                }
            }
    }
}
