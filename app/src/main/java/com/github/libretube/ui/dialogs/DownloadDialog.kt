package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
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
import com.github.libretube.extensions.sanitize
import com.github.libretube.services.DownloadService
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.MetadataHelper
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

        binding.audioRadio.setOnClickListener {
            binding.videoSpinner.visibility = View.GONE
        }

        binding.videoRadio.setOnClickListener {
            binding.videoSpinner.visibility = View.VISIBLE
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
        binding.fileName.setText(streams.title.toString())

        val vidName = arrayListOf<String>()
        val videoUrl = arrayListOf<String>()

        // add empty selection
        vidName.add(getString(R.string.no_video))
        videoUrl.add("")

        // add all available video streams
        for (vid in streams.videoStreams!!) {
            if (vid.url != null) {
                val name = vid.quality + " " + vid.format
                vidName.add(name)
                videoUrl.add(vid.url!!)
            }
        }

        val audioName = arrayListOf<String>()
        val audioUrl = arrayListOf<String>()

        // add empty selection
        audioName.add(getString(R.string.no_audio))
        audioUrl.add("")

        // add all available audio streams
        for (audio in streams.audioStreams!!) {
            if (audio.url != null) {
                val name = audio.quality + " " + audio.format
                audioName.add(name)
                audioUrl.add(audio.url!!)
            }
        }

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

        // initialize the audio sources
        val audioArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            audioName
        )
        audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.audioSpinner.adapter = audioArrayAdapter
        if (binding.audioSpinner.size >= 1) binding.audioSpinner.setSelection(1)

        binding.download.setOnClickListener {
            if (binding.fileName.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.invalid_filename, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vidUrl = videoUrl[binding.videoSpinner.selectedItemPosition]
            val audUrl = audioUrl[binding.audioSpinner.selectedItemPosition]

            if (audUrl == "" && vidUrl == "") {
                Toast.makeText(context, R.string.nothing_selected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fileName = binding.fileName.text.toString().sanitize()

            val metadataHelper = MetadataHelper(requireContext())
            metadataHelper.createMetadata(fileName, streams)
            streams.thumbnailUrl?.let { thumbnailUrl ->
                ImageHelper.downloadImage(
                    requireContext(),
                    thumbnailUrl,
                    fileName
                )
            }

            val intent = Intent(context, DownloadService::class.java)

            intent.putExtra(
                "videoName",
                fileName
            )
            intent.putExtra(
                "videoUrl",
                vidUrl
            )
            intent.putExtra(
                "audioUrl",
                audUrl
            )

            context?.startService(intent)
            dismiss()
        }
    }
}
