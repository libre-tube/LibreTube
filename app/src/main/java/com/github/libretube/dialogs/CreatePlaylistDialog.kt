package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.obj.Playlists
import com.github.libretube.util.RetrofitInstance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.IOException
import retrofit2.HttpException

class CreatePlaylistDialog : DialogFragment() {
    val TAG = "CreatePlaylistDialog"
    private var token: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            val view: View = inflater.inflate(R.layout.dialog_create_playlist, null)

            val typedValue = TypedValue()
            this.requireActivity().theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val hexColor = String.format("#%06X", (0xFFFFFF and typedValue.data))
            val appName = HtmlCompat.fromHtml(
                "Libre<span  style='color:$hexColor';>Tube</span>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            view.findViewById<TextView>(R.id.title).text = appName

            val cancelBtn = view.findViewById<Button>(R.id.cancel_button)
            cancelBtn.setOnClickListener {
                dismiss()
            }

            val sharedPref = context?.getSharedPreferences("token", Context.MODE_PRIVATE)
            token = sharedPref?.getString("token", "")!!

            val playlistName = view.findViewById<TextInputEditText>(R.id.playlist_name)
            val createPlaylistBtn = view.findViewById<Button>(R.id.create_new_playlist)
            createPlaylistBtn.setOnClickListener {
                // avoid creating the same playlist multiple times by spamming the button
                createPlaylistBtn.setOnClickListener(null)
                val listName = playlistName.text.toString()
                if (listName != "") {
                    createPlaylist(listName)
                } else {
                    Toast.makeText(context, R.string.emptyPlaylistName, Toast.LENGTH_LONG).show()
                }
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun createPlaylist(name: String) {
        fun run() {
            lifecycleScope.launchWhenCreated {
                val response = try {
                    RetrofitInstance.api.createPlaylist(token, Playlists(name = name))
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG, "IOException, you might not have internet connection")
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG, "HttpException, unexpected response $e")
                    Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                    return@launchWhenCreated
                }
                if (response != null) {
                    Toast.makeText(context, R.string.playlistCreated, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_SHORT)
                        .show()
                }
                // tell the Subscription Activity to fetch the playlists again
                setFragmentResult("fetchPlaylists", bundleOf("" to ""))
                dismiss()
            }
        }
        run()
    }
}
