package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.local.StreamsExtractor
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.getWhileDigit
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.parcelable.DownloadData
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadDialog : DialogFragment() {
    private lateinit var videoId: String
    private var onDownloadConfirm = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId = arguments?.getString(IntentData.videoId)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDownloadBinding.inflate(layoutInflater)

        fetchAvailableSources(binding)

        binding.fileName.filters += InputFilter { source, start, end, _, _, _ ->
            if (source.isNullOrBlank()) {
                return@InputFilter null
            }

            // Extract actual source
            val actualSource = source.subSequence(start, end)
            // Filter out unsupported characters
            val filtered = actualSource.filterNot {
                TextUtils.RESERVED_CHARS.contains(it, true)
            }
            // Check if something was filtered out
            return@InputFilter if (actualSource.length != filtered.length) {
                filtered
            } else {
                null
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.download)
            .setView(binding.root)
            .setPositiveButton(R.string.download, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    onDownloadConfirm.invoke()
                }
            }
    }

    private fun fetchAvailableSources(binding: DialogDownloadBinding) {
        lifecycleScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    StreamsExtractor.extractStreams(videoId)
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.stackTraceToString())
                val context = context ?: return@launch
                val errorMessage = StreamsExtractor.getExtractorErrorMessageString(context, e)
                context.toastFromMainDispatcher(errorMessage)
                return@launch
            }
            initDownloadOptions(binding, response)
        }
    }

    private fun initDownloadOptions(binding: DialogDownloadBinding, streams: Streams) {
        binding.fileName.setText(streams.title)

        val videoStreams = streams.videoStreams.filter {
            !it.url.isNullOrEmpty()
        }.filter { !it.format.orEmpty().contains("HLS") }.sortedByDescending {
            it.quality.getWhileDigit()
        }

        val audioStreams = streams.audioStreams.filter {
            !it.url.isNullOrEmpty()
        }.sortedByDescending {
            it.quality.getWhileDigit()
        }

        val subtitles = streams.subtitles
            .filter { !it.url.isNullOrEmpty() && !it.name.isNullOrEmpty() }
            .sortedBy { it.name }

        if (subtitles.isEmpty()) binding.subtitleSpinner.isGone = true

        binding.videoSpinner.items = videoStreams.map {
            val fileSize = Formatter.formatShortFileSize(context, it.contentLength)
            "${it.quality} ${it.codec} ($fileSize)"
        }.toMutableList().also {
            it.add(0, getString(R.string.no_video))
        }

        binding.audioSpinner.items = audioStreams.map {
            val fileSize = it.contentLength
                .takeIf { l -> l > 0 }
                ?.let { cl -> Formatter.formatShortFileSize(context, cl) }
            val infoStr = listOfNotNull(it.audioTrackLocale, fileSize)
                .joinToString(", ")
            "${it.quality} ${it.format} ($infoStr)"
        }.toMutableList().also {
            it.add(0, getString(R.string.no_audio))
        }

        binding.subtitleSpinner.items = subtitles.map { it.name.orEmpty() }.toMutableList().also {
            it.add(0, getString(R.string.no_subtitle))
        }

        restorePreviousSelections(binding, videoStreams, audioStreams, subtitles)

        onDownloadConfirm = onDownloadConfirm@{
            val fileName = binding.fileName.text.toString()
            if (fileName.isBlank()) {
                Toast.makeText(context, R.string.invalid_filename, Toast.LENGTH_SHORT).show()
                return@onDownloadConfirm
            }

            if (fileName.toByteArray().size > MAX_FILE_NAME_BYTES - 32) { // reserve 32 bytes for quality and extension
                Toast.makeText(context, R.string.filename_too_long, Toast.LENGTH_SHORT).show()
                return@onDownloadConfirm
            }

            val videoPosition = binding.videoSpinner.selectedItemPosition - 1
            val audioPosition = binding.audioSpinner.selectedItemPosition - 1
            val subtitlePosition = binding.subtitleSpinner.selectedItemPosition - 1

            if (listOf(videoPosition, audioPosition, subtitlePosition).all { it == -1 }) {
                Toast.makeText(context, R.string.nothing_selected, Toast.LENGTH_SHORT).show()
                return@onDownloadConfirm
            }

            val videoStream = videoStreams.getOrNull(videoPosition)
            val audioStream = audioStreams.getOrNull(audioPosition)
            val subtitle = subtitles.getOrNull(subtitlePosition)

            saveSelections(videoStream, audioStream, subtitle)

            val downloadData = DownloadData(
                videoId = videoId,
                fileName = binding.fileName.text?.toString().orEmpty(),
                videoFormat = videoStream?.format,
                videoQuality = videoStream?.quality,
                audioFormat = audioStream?.format,
                audioQuality = audioStream?.quality,
                audioLanguage = audioStream?.audioTrackLocale,
                subtitleCode = subtitle?.code
            )
            DownloadHelper.startDownloadService(requireContext(), downloadData)

            dismiss()
        }
    }

    /**
     * Save the download selection to the preferences
     */
    private fun saveSelections(
        videoStream: PipedStream?,
        audioStream: PipedStream?,
        subtitle: Subtitle?
    ) {
        PreferenceHelper.putString(SUBTITLE_LANGUAGE, subtitle?.code.orEmpty())
        PreferenceHelper.putString(VIDEO_DOWNLOAD_FORMAT, videoStream?.format.orEmpty())
        PreferenceHelper.putString(VIDEO_DOWNLOAD_QUALITY, videoStream?.quality.orEmpty())
        PreferenceHelper.putString(AUDIO_DOWNLOAD_FORMAT, audioStream?.format.orEmpty())
        PreferenceHelper.putString(AUDIO_DOWNLOAD_QUALITY, audioStream?.quality.orEmpty())
    }

    private fun getSel(key: String) = PreferenceHelper.getString(key, "")

    /**
     * Restore the download selections from a previous session
     */
    private fun restorePreviousSelections(
        binding: DialogDownloadBinding,
        videoStreams: List<PipedStream>,
        audioStreams: List<PipedStream>,
        subtitles: List<Subtitle>
    ) {
        getStreamSelection(
            videoStreams,
            getSel(VIDEO_DOWNLOAD_QUALITY),
            getSel(VIDEO_DOWNLOAD_FORMAT)
        )?.let {
            binding.videoSpinner.selectedItemPosition = it + 1
        }
        getStreamSelection(
            audioStreams,
            getSel(AUDIO_DOWNLOAD_QUALITY),
            getSel(AUDIO_DOWNLOAD_FORMAT)
        )?.let {
            binding.audioSpinner.selectedItemPosition = it + 1
        }

        subtitles.indexOfFirst { it.code == getSel(SUBTITLE_LANGUAGE) }.takeIf { it != -1 }?.let {
            binding.subtitleSpinner.selectedItemPosition = it + 1
        }
    }

    private fun getStreamSelection(
        streams: List<PipedStream>,
        quality: String,
        format: String
    ): Int? {
        if (quality.isBlank()) return null

        streams.forEachIndexed { index, pipedStream ->
            if (quality == pipedStream.quality && format == pipedStream.format) return index
        }

        streams.forEachIndexed { index, pipedStream ->
            if (quality == pipedStream.quality) return index
        }

        val qualityInt = quality.getWhileDigit() ?: return null

        streams.forEachIndexed { index, pipedStream ->
            if ((pipedStream.quality.getWhileDigit() ?: Int.MAX_VALUE) < qualityInt) return index
        }

        return null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        setFragmentResult(DOWNLOAD_DIALOG_DISMISSED_KEY, bundleOf())
    }

    companion object {
        /**
         * Max file name length at Android systems
         */
        private const val MAX_FILE_NAME_BYTES = 255

        private const val VIDEO_DOWNLOAD_QUALITY = "video_download_quality"
        private const val VIDEO_DOWNLOAD_FORMAT = "video_download_format"
        private const val AUDIO_DOWNLOAD_QUALITY = "audio_download_quality"
        private const val AUDIO_DOWNLOAD_FORMAT = "audio_download_format"
        private const val SUBTITLE_LANGUAGE = "subtitle_download_language"

        const val DOWNLOAD_DIALOG_DISMISSED_KEY = "download_dialog_dismissed_key"
    }
}
