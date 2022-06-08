package com.github.libretube.dialogs

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.github.libretube.DownloadService
import com.github.libretube.MainActivity
import com.github.libretube.R
import com.github.libretube.obj.Streams
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DownloadDialog : DialogFragment() {
    private val TAG = "DownloadDialog"
    var streams: Streams = Streams()
    var selectedVideo = 0
    var selectedAudio = 0
    var duration = 0
    private lateinit var videoId: String
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            streams = arguments?.getParcelable("streams")!!
            videoId = arguments?.getString("video_id")!!

            val mainActivity = activity as MainActivity
            Log.e(TAG, "download button clicked!")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("myz", "" + Build.VERSION.SDK_INT)
                if (!Environment.isExternalStorageManager()) {
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        ),
                        1
                    ) // permission request code is just an int
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        mainActivity,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        1
                    )
                }
            }
            var vidName = arrayListOf<String>()
            vidName.add("No video")
            var vidUrl = arrayListOf<String>()
            vidUrl.add("")
            for (vid in streams.videoStreams!!) {
                val name = vid.quality + " " + vid.format
                vidName.add(name)
                vidUrl.add(vid.url!!)
            }
            var audioName = arrayListOf<String>()
            audioName.add("No audio")
            var audioUrl = arrayListOf<String>()
            audioUrl.add("")
            for (audio in streams.audioStreams!!) {
                val name = audio.quality + " " + audio.format
                audioName.add(name)
                audioUrl.add(audio.url!!)
            }

            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            var view: View = inflater.inflate(R.layout.dialog_download, null)
            val videoSpinner = view.findViewById<Spinner>(R.id.video_spinner)
            val videoArrayAdapter = ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vidName
            )
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
            val audioArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                audioName
            )
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

            view.findViewById<Button>(R.id.download).setOnClickListener {
                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra("videoId", videoId)
                intent.putExtra("videoUrl", vidUrl[selectedVideo])
                intent.putExtra("audioUrl", audioUrl[selectedAudio])
                intent.putExtra("duration", duration)
                // intent.putExtra("command","-y -i ${response.videoStreams[which].url} -i ${response.audioStreams!![0].url} -c copy ${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${videoId}.mkv")
                context?.startService(intent)
                dismiss()
            }

            val typedValue = TypedValue()
            this.requireActivity().theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            view.findViewById<TextView>(R.id.title).text = appName

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
