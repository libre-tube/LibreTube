package com.github.libretube

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

class CreatePlaylistDialog : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootView: View = inflater.inflate(R.layout.dialog_create_playlist, container, false)

        val cancelBtn = rootView.findViewById<Button>(R.id.cancel_button)
        cancelBtn.setOnClickListener {
            dismiss()
        }

        val playlistName = rootView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.playlist_name)
        val createPlaylistBtn = rootView.findViewById<Button>(R.id.create_new_playlist)
        createPlaylistBtn.setOnClickListener {
            var listName = playlistName.text.toString()
            if (listName != "") {
                setFragmentResult("key_parent", bundleOf("playlistName" to "$listName"))
                dismiss()
            } else {
                Toast.makeText(context, R.string.emptyPlaylistName, Toast.LENGTH_LONG).show()
            }
        }

        return rootView
    }
}
