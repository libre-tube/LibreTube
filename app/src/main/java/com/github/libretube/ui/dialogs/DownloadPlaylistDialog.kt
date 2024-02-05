package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogDownloadPlaylistBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.getWhileDigit
import com.github.libretube.extensions.serializable
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.services.PlaylistDownloadEnqueueService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadPlaylistDialog : DialogFragment() {
    private lateinit var playlistId: String
    private lateinit var playlistName: String
    private lateinit var playlistType: PlaylistType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlistId = requireArguments().getString(IntentData.playlistId)!!
        playlistName = requireArguments().getString(IntentData.playlistName)!!
        playlistType = requireArguments().serializable(IntentData.playlistType)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDownloadPlaylistBinding.inflate(layoutInflater)

        val possibleVideoQualities = resources.getStringArray(R.array.defres).toList().let {
            // remove the automatic quality entry
            it.subList(1, it.size)
        }
        val possibleAudioQualities = resources.getStringArray(R.array.audioQualityBitrates)
        val availableLanguages = LocaleHelper.getAvailableLocales()

        binding.videoSpinner.items = listOf(getString(R.string.no_video)) + possibleVideoQualities
        binding.audioSpinner.items = listOf(getString(R.string.no_audio)) + possibleAudioQualities
        binding.subtitleSpinner.items =
            listOf(getString(R.string.no_subtitle)) + availableLanguages.map { it.name }
        binding.audioLanguageSpinner.items =
            listOf(getString(R.string.default_language)) + availableLanguages.map { it.name }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.download_playlist) + ": " + playlistName)
            .setMessage(R.string.download_playlist_note)
            .setView(binding.root)
            .setPositiveButton(R.string.download) { _, _ ->
                with(binding) {
                    val maxVideoQuality = if (videoSpinner.selectedItemPosition >= 1)
                        possibleVideoQualities[videoSpinner.selectedItemPosition - 1]
                            .getWhileDigit() else null

                    val maxAudioQuality = if (audioSpinner.selectedItemPosition >= 1)
                        possibleAudioQualities[audioSpinner.selectedItemPosition - 1]
                            .getWhileDigit() else null

                    val captionLanguage = if (subtitleSpinner.selectedItemPosition >= 1)
                        availableLanguages[subtitleSpinner.selectedItemPosition - 1].code
                    else null

                    val audioLanguage = if (audioLanguageSpinner.selectedItemPosition >= 1)
                        availableLanguages[audioLanguageSpinner.selectedItemPosition - 1].code
                    else null

                    if (maxVideoQuality == null && maxAudioQuality == null) {
                        Toast.makeText(context, R.string.nothing_selected, Toast.LENGTH_SHORT)
                            .show()
                        return@setPositiveButton
                    }

                    val downloadEnqueueIntent =
                        Intent(requireContext(), PlaylistDownloadEnqueueService::class.java)
                            .putExtra(IntentData.playlistId, playlistId)
                            .putExtra(IntentData.playlistType, playlistType)
                            .putExtra(IntentData.playlistName, playlistName)
                            .putExtra(IntentData.audioLanguage, audioLanguage)
                            .putExtra(IntentData.maxVideoQuality, maxVideoQuality)
                            .putExtra(IntentData.maxAudioQuality, maxAudioQuality)
                            .putExtra(IntentData.captionLanguage, captionLanguage)

                    ContextCompat.startForegroundService(requireContext(), downloadEnqueueIntent)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

    }
}