package com.github.libretube.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.services.DownloadService

class DownloadDialogFragment : DialogFragment() {
    private val TAG = "DownloadDialog"
    var vidName = arrayListOf<String>()
    var vidUrl = arrayListOf<String>()
    var audioName = arrayListOf<String>()
    var audioUrl = arrayListOf<String>()
    var selectedVideo = 0
    var selectedAudio = 0
    var extension = ".mkv"
    var duration = 0
    private lateinit var videoId: String
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
            var view: View = inflater.inflate(R.layout.dialog_download, null)
            val videoSpinner = view.findViewById<Spinner>(R.id.video_spinner)
            val videoArrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, vidName)
            videoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            videoSpinner.adapter = videoArrayAdapter
            videoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    selectedVideo = position
                    Log.d(TAG, selectedVideo.toString())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            val audioSpinner = view.findViewById<Spinner>(R.id.audio_spinner)
            val audioArrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, audioName)
            audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            audioSpinner.adapter = audioArrayAdapter
            audioSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    selectedAudio = position
                    Log.d(TAG, selectedAudio.toString())
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            val radioGroup = view.findViewById<RadioGroup>(R.id.radioGp)
            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                val radio: RadioButton = view.findViewById(checkedId)
                extension = radio.text.toString()
                Log.d(TAG, extension)
            }
            view.findViewById<Button>(R.id.download).setOnClickListener {
                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra("videoId", videoId)
                intent.putExtra("videoUrl", vidUrl[selectedVideo])
                intent.putExtra("audioUrl", audioUrl[selectedAudio])
                intent.putExtra("duration", duration)
                intent.putExtra("extension", extension)
                // intent.putExtra("command","-y -i ${response.videoStreams[which].url} -i ${response.audioStreams!![0].url} -c copy ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${videoId}.mkv")
                context?.startService(intent)
                dismiss()
            }
            builder.setView(view)
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
