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
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.services.PlaylistDownloadEnqueueService
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadPlaylistDialog : DialogFragment() {
    private lateinit var playlistId: String
    private lateinit var playlistName: String
    private lateinit var playlistType: PlaylistType

    private val availableLanguages = LocaleHelper.getAvailableLocales()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playlistId = requireArguments().getString(IntentData.playlistId)!!
        playlistName = requireArguments().getString(IntentData.playlistName)
            ?: TextUtils.getFileSafeTimeStampNow()
        playlistType = requireArguments().serializable(IntentData.playlistType)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDownloadPlaylistBinding.inflate(layoutInflater)

        val possibleVideoQualities = resources.getStringArray(R.array.defres).toList().let {
            // remove the automatic quality entry
            it.subList(1, it.size)
        }
        val possibleAudioQualities = resources.getStringArray(R.array.audioQualityBitrates)
            .toList()

        binding.videoSpinner.items = listOf(getString(R.string.no_video)) + possibleVideoQualities
        binding.audioSpinner.items = listOf(getString(R.string.no_audio)) + possibleAudioQualities
        binding.subtitleSpinner.items =
            listOf(getString(R.string.no_subtitle)) + availableLanguages.map { it.name }
        binding.audioLanguageSpinner.items =
            listOf(getString(R.string.default_language)) + availableLanguages.map { it.name }

        restoreSelections(binding)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.download_playlist) + ": " + playlistName)
            .setView(binding.root)
            .setPositiveButton(R.string.download) { _, _ ->
                with(binding) {
                    val maxVideoQuality = possibleVideoQualities.getOrNull(videoSpinner.selectedItemPosition - 1)
                        .getWhileDigit()
                    val maxAudioQuality = possibleAudioQualities.getOrNull(audioSpinner.selectedItemPosition - 1)
                        .getWhileDigit()
                    val captionLanguage = availableLanguages.getOrNull(subtitleSpinner.selectedItemPosition - 1)?.code
                    val audioLanguage = availableLanguages.getOrNull(audioLanguageSpinner.selectedItemPosition - 1)?.code

                    saveSelections(binding)

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

    private fun saveSelections(binding: DialogDownloadPlaylistBinding) {
        binding.audioSpinner.getSelectionIfNotFirst().let { item ->
            PreferenceHelper.putString(PLAYLIST_DOWNLOAD_AUDIO_QUALITY, item.orEmpty())
        }
        binding.videoSpinner.getSelectionIfNotFirst().let { item ->
            PreferenceHelper.putString(PLAYLIST_DOWNLOAD_VIDEO_QUALITY, item.orEmpty())
        }
        binding.audioLanguageSpinner.selectedItemPosition.let { index ->
            val language = availableLanguages.getOrNull(index - 1)
            PreferenceHelper.putString(PLAYLIST_DOWNLOAD_AUDIO_LANGUAGE, language?.code.orEmpty())
        }
        binding.subtitleSpinner.selectedItemPosition.let { index ->
            val language = availableLanguages.getOrNull(index - 1)
            PreferenceHelper.putString(PLAYLIST_DOWNLOAD_CAPTION_LANGUAGE, language?.code.orEmpty())
        }
    }

    private fun restoreSelections(binding: DialogDownloadPlaylistBinding) {
        val videoQuality = PreferenceHelper.getString(PLAYLIST_DOWNLOAD_VIDEO_QUALITY, "")
        binding.videoSpinner.setSelection(videoQuality)

        val audioQuality = PreferenceHelper.getString(PLAYLIST_DOWNLOAD_AUDIO_QUALITY, "")
        binding.audioSpinner.setSelection(audioQuality)

        val audioLanguage = PreferenceHelper.getString(PLAYLIST_DOWNLOAD_AUDIO_LANGUAGE, "")
        binding.audioLanguageSpinner.setSelection(audioLanguage)

        val captionLanguage = PreferenceHelper.getString(PLAYLIST_DOWNLOAD_CAPTION_LANGUAGE, "")
        binding.subtitleSpinner.setSelection(captionLanguage)
    }

    companion object {
        private const val PLAYLIST_DOWNLOAD_VIDEO_QUALITY = "playlist_download_video_quality"
        private const val PLAYLIST_DOWNLOAD_AUDIO_QUALITY = "playlist_download_audio_quality"
        private const val PLAYLIST_DOWNLOAD_AUDIO_LANGUAGE = "playlist_download_audio_language"
        private const val PLAYLIST_DOWNLOAD_CAPTION_LANGUAGE = "playlist_download_caption_language"
    }
}
