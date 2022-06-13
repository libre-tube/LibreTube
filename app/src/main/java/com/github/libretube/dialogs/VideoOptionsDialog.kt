package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.BackgroundMode
import com.github.libretube.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsDialog(private val videoId: String, context: Context) : DialogFragment() {
    /**
     * List that stores the different menu options. In the future could be add more options here.
     */
    private val list = listOf(
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
                    list
                )
            ) { dialog, which ->
                // For now, this checks the position of the option with the position that is in the
                // list. I don't like it, but we will do like this for now.
                when (which) {
                    // This for example will be the "Background mode" option
                    0 -> {
                        BackgroundMode.getInstance()
                            .playOnBackgroundMode(requireContext(), videoId, 0)
                    }
                    // Add Video to Playlist Dialog
                    1 -> {
                        val sharedPref = context?.getSharedPreferences(
                            "token",
                            Context.MODE_PRIVATE
                        )
                        val token = sharedPref?.getString("token", "")
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
                    2 -> {
                        val shareDialog = ShareDialog(videoId)
                        // using parentFragmentManager is important here
                        shareDialog.show(parentFragmentManager, "ShareDialog")
                    }
                    else -> {
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }

    companion object {
        const val TAG = "VideoOptionsDialog"
    }
}
