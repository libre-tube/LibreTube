package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.format.Formatter
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.getWhileDigit
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.parcelable.DownloadData
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val response = try {
                    withContext(Dispatchers.IO) {
                        RetrofitInstance.api.getStreams(videoId)
                    }
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                }
                initDownloadOptions(binding, response)
            }
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

        // initialize the video sources
        val videoArrayAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            videoStreams.map {
                val fileSize = Formatter.formatShortFileSize(context, it.contentLength)
                "${it.quality} ${it.codec} ($fileSize)"
            }.toMutableList().also {
                it.add(0, getString(R.string.no_video))
            }
        )

        val audioArrayAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            audioStreams.map {
                val fileSize = it.contentLength
                    .takeIf { l -> l > 0 }
                    ?.let { cl -> Formatter.formatShortFileSize(context, cl) }
                val infoStr = listOfNotNull(it.audioTrackLocale, fileSize)
                    .joinToString(", ")
                "${it.quality} ${it.format} ($infoStr)"
            }.toMutableList().also {
                it.add(0, getString(R.string.no_audio))
            }
        )

        val subtitleArrayAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            subtitles.map { it.name.orEmpty() }.toMutableList().also {
                it.add(0, getString(R.string.no_subtitle))
            }
        )

        binding.videoSpinner.adapter = videoArrayAdapter
        binding.audioSpinner.adapter = audioArrayAdapter
        binding.subtitleSpinner.adapter = subtitleArrayAdapter

        restorePreviousSelections(binding, videoStreams, audioStreams, subtitles)

        onDownloadConfirm = onDownloadConfirm@{
            if (binding.fileName.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.invalid_filename, Toast.LENGTH_SHORT).show()
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
            binding.videoSpinner.setSelection(it + 1)
        }
        getStreamSelection(
            audioStreams,
            getSel(AUDIO_DOWNLOAD_QUALITY),
            getSel(AUDIO_DOWNLOAD_FORMAT)
        )?.let {
            binding.audioSpinner.setSelection(it + 1)
        }

        subtitles.indexOfFirst { it.code == getSel(SUBTITLE_LANGUAGE) }.takeIf { it != -1 }?.let {
            binding.subtitleSpinner.setSelection(it + 1)
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

    companion object {
        private const val VIDEO_DOWNLOAD_QUALITY = "video_download_quality"
        private const val VIDEO_DOWNLOAD_FORMAT = "video_download_format"
        private const val AUDIO_DOWNLOAD_QUALITY = "audio_download_quality"
        private const val AUDIO_DOWNLOAD_FORMAT = "audio_download_format"
        private const val SUBTITLE_LANGUAGE = "subtitle_download_language"
    }
}
