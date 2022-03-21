package com.github.libretube.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.service.DownloadService

private const val TAG = "DownloadDialogFragment"

class DownloadDialogFragment : DialogFragment() {
    private lateinit var binding: DialogDownloadBinding
    private lateinit var videoId: String
    private var vidName = arrayListOf<String>()
    private var vidUrl = arrayListOf<String>()
    private var audioName = arrayListOf<String>()
    private var audioUrl = arrayListOf<String>()
    private var extension = ".mkv"
    private var selectedVideo = 0
    private var selectedAudio = 0
    private var duration = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDownloadBinding.inflate(layoutInflater)

        return activity?.let {
            vidName = arguments?.getStringArrayList("videoName") as ArrayList<String>
            vidUrl = arguments?.getStringArrayList("videoUrl") as ArrayList<String>
            audioName = arguments?.getStringArrayList("audioName") as ArrayList<String>
            audioUrl = arguments?.getStringArrayList("audioUrl") as ArrayList<String>
            duration = arguments?.getInt("duration")!!
            videoId = arguments?.getString("videoId")!!
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view: View = inflater.inflate(R.layout.dialog_download, null)
            val videoArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vidName
            )
            videoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.videoSpinner.adapter = videoArrayAdapter
            binding.videoSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long,
                    ) {
                        selectedVideo = position
                        Log.d(TAG, selectedVideo.toString())
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // no op
                    }
                }
            val audioArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                audioName
            )
            audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.audioSpinner.adapter = audioArrayAdapter
            binding.audioSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View,
                        position: Int,
                        id: Long,
                    ) {
                        selectedAudio = position
                        Log.d(TAG, selectedAudio.toString())
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // no op
                    }
                }
            binding.radioGp.setOnCheckedChangeListener { _, checkedId ->
                extension = when (checkedId) {
                    R.id.mkv -> binding.mkv.text.toString()
                    R.id.mp4 -> binding.mp4.text.toString()
                    else -> {
                        ""
                    }
                }
                Log.d(TAG, extension)
            }
            binding.btnDownload.setOnClickListener {
                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra("videoId", videoId)
                intent.putExtra("videoUrl", vidUrl[selectedVideo])
                intent.putExtra("audioUrl", audioUrl[selectedAudio])
                intent.putExtra("duration", duration)
                intent.putExtra("extension", extension)
                context?.startService(intent)
                dismiss()
            }
            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        vidName.clear()
        vidUrl.clear()
        audioUrl.clear()
        audioName.clear()
        super.onDestroy()
    }
}
