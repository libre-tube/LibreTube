package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.Globals
import com.github.libretube.R
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.services.BackgroundMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsDialog(private val videoId: String, context: Context) : DialogFragment() {
    private val TAG = "VideoOptionsDialog"

    /**
     * List that stores the different menu options. In the future could be add more options here.
     */
    private val optionsList = listOf(
        context.getString(R.string.playOnBackground),
        context.getString(R.string.addToPlaylist),
        context.getString(R.string.share)
    )

    /**
     * Dialog that returns a [MaterialAlertDialogBuilder] showing a menu of options.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.video_options_dialog_item,
                    optionsList
                )
            ) { _, which ->
                when (optionsList[which]) {
                    // Start the background mode
                    context?.getString(R.string.playOnBackground) -> {
                        if (Globals.backgroundModeIntent != null) {
                            activity?.stopService(Globals.backgroundModeIntent)
                        }
                        val intent = Intent(context, BackgroundMode::class.java)
                        intent.putExtra("videoId", videoId)
                        Globals.backgroundModeIntent = intent
                        activity?.startService(intent)
                    }
                    // Add Video to Playlist Dialog
                    context?.getString(R.string.addToPlaylist) -> {
                        val token = PreferenceHelper.getToken()
                        if (token != "") {
                            val newFragment = AddtoPlaylistDialog()
                            val bundle = Bundle()
                            bundle.putString("videoId", videoId)
                            newFragment.arguments = bundle
                            newFragment.show(parentFragmentManager, "AddToPlaylist")
                        } else {
                            Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                        }
                    }
                    context?.getString(R.string.share) -> {
                        val shareDialog = ShareDialog(videoId, false)
                        // using parentFragmentManager is important here
                        shareDialog.show(parentFragmentManager, "ShareDialog")
                    }
                }
            }
            .show()
    }
}
