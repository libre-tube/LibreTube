package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogTextPreferenceBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet.Companion.PLAYLIST_OPTIONS_REQUEST_KEY
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RenamePlaylistDialog : DialogFragment() {
    private lateinit var playlistId: String
    private lateinit var currentPlaylistName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString(IntentData.playlistId)!!
            currentPlaylistName = it.getString(IntentData.playlistName)!!
        }
    }
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
                            setFragmentResult(
                                PLAYLIST_OPTIONS_REQUEST_KEY,
                                bundleOf(IntentData.playlistName to newPlaylistName)
                            )
                        } else {
                            appContext.toastFromMainDispatcher(R.string.server_error)
                        }
                        dismiss()
                    }
                }
            }
    }
}
