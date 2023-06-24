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

class PlaylistDescriptionDialog(
    private val playlistId: String,
    private val currentPlaylistDescription: String,
    private val onSuccess: (String) -> Unit
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTextPreferenceBinding.inflate(layoutInflater)
        binding.input.inputType = InputType.TYPE_CLASS_TEXT
        binding.input.hint = getString(R.string.playlist_description)
        binding.input.setText(currentPlaylistDescription)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_playlist_description)
            .setView(binding.root)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val newDescription = binding.input.text?.toString()
                    if (newDescription.isNullOrEmpty()) {
                        Toast.makeText(
                            context,
                            R.string.emptyPlaylistDescription,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    if (newDescription == currentPlaylistDescription) {
                        dismiss()
                        return@setOnClickListener
                    }
                    val appContext = requireContext().applicationContext

                    lifecycleScope.launch {
                        requireDialog().hide()
                        val success = try {
                            withContext(Dispatchers.IO) {
                                PlaylistsHelper.changePlaylistDescription(
                                    playlistId,
                                    newDescription
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG(), e.toString())
                            e.localizedMessage?.let { appContext.toastFromMainDispatcher(it) }
                            return@launch
                        }
                        if (success) {
                            appContext.toastFromMainDispatcher(R.string.success)
                            onSuccess.invoke(newDescription)
                        } else {
                            appContext.toastFromMainDispatcher(R.string.server_error)
                        }
                        dismiss()
                    }
                }
            }
    }
}
