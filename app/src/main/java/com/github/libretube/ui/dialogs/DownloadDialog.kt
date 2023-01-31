package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.size
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Streams
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.util.TextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import retrofit2.HttpException

class DownloadDialog(
    private val videoId: String
) : DialogFragment() {
    private lateinit var binding: DialogDownloadBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDownloadBinding.inflate(layoutInflater)

        fetchAvailableSources()

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
            .show()
    }

    private fun fetchAvailableSources() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getStreams(videoId)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            initDownloadOptions(response)
        }
    }

    private fun initDownloadOptions(streams: Streams) {
        binding.fileName.setText(streams.title)

        val vidName = arrayListOf<String>()

        // add empty selection
        vidName.add(getString(R.string.no_video))

        // add all available video streams
        for (vid in streams.videoStreams) {
            if (vid.url != null) {
                val name = vid.quality + " " + vid.format
                vidName.add(name)
            }
        }

        val audioName = arrayListOf<String>()

        // add empty selection
        audioName.add(getString(R.string.no_audio))

        // add all available audio streams
        for (audio in streams.audioStreams) {
            if (audio.url != null) {
                val name = audio.quality + " " + audio.format
                audioName.add(name)
            }
        }

        val subtitleName = arrayListOf<String>()

        // add empty selection
        subtitleName.add(getString(R.string.no_subtitle))

        // add all available subtitles
        for (subtitle in streams.subtitles) {
            if (subtitle.url != null) {
                subtitleName.add(subtitle.name.toString())
            }
        }

        if (subtitleName.size == 1) binding.subtitleSpinner.visibility = View.GONE

        // initialize the video sources
        val videoArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            vidName
        )
        videoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.videoSpinner.adapter = videoArrayAdapter
        if (binding.videoSpinner.size >= 1) binding.videoSpinner.setSelection(1)
        if (binding.audioSpinner.size >= 1) binding.audioSpinner.setSelection(1)
        if (binding.subtitleSpinner.size >= 1) binding.subtitleSpinner.setSelection(1)

        // initialize the audio sources
        val audioArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            audioName
        )
        audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.audioSpinner.adapter = audioArrayAdapter
        if (binding.audioSpinner.size >= 1) binding.audioSpinner.setSelection(1)

        // initialize the subtitle sources
        val subtitleArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subtitleName
        )
        subtitleArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.subtitleSpinner.adapter = subtitleArrayAdapter
        if (binding.subtitleSpinner.size >= 1) binding.subtitleSpinner.setSelection(1)

        binding.download.setOnClickListener {
            if (binding.fileName.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.invalid_filename, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val videoPosition = binding.videoSpinner.selectedItemPosition - 1
            val audioPosition = binding.audioSpinner.selectedItemPosition - 1
            val subtitlePosition = binding.subtitleSpinner.selectedItemPosition - 1

            if (videoPosition == -1 && audioPosition == -1 && subtitlePosition == -1) {
                Toast.makeText(context, R.string.nothing_selected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val videoStream = when (videoPosition) {
                -1 -> null
                else -> streams.videoStreams[videoPosition]
            }
            val audioStream = when (audioPosition) {
                -1 -> null
                else -> streams.audioStreams[audioPosition]
            }
            val subtitle = when (subtitlePosition) {
                -1 -> null
                else -> streams.subtitles[subtitlePosition]
            }

            DownloadHelper.startDownloadService(
                context = requireContext(),
                videoId = videoId,
                fileName = binding.fileName.text.toString(),
                videoFormat = videoStream?.format,
                videoQuality = videoStream?.quality,
                audioFormat = audioStream?.format,
                audioQuality = audioStream?.quality,
                subtitleCode = subtitle?.code
            )

            dismiss()
        }
    }
}
