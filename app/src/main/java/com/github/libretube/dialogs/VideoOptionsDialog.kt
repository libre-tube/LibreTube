package com.github.libretube.dialogs

import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.Globals
import com.github.libretube.PLAYER_NOTIFICATION_ID
import com.github.libretube.R
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.util.BackgroundHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsDialog(
    private val videoId: String
) : DialogFragment() {

    /**
     * Dialog that returns a [MaterialAlertDialogBuilder] showing a menu of options.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        /**
         * List that stores the different menu options. In the future could be add more options here.
         */
        val optionsList = mutableListOf(
            context?.getString(R.string.playOnBackground),
            context?.getString(R.string.addToPlaylist),
            context?.getString(R.string.download),
            context?.getString(R.string.share)
        )

        /**
         * Check whether the player is running by observing the notification
         */
        try {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.activeNotifications.forEach {
                if (it.id == PLAYER_NOTIFICATION_ID) {
                    optionsList += context?.getString(R.string.add_to_queue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setNegativeButton(R.string.cancel, null)
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
                        BackgroundHelper.playOnBackground(requireContext(), videoId)
                    }
                    // Add Video to Playlist Dialog
                    context?.getString(R.string.addToPlaylist) -> {
                        val token = PreferenceHelper.getToken()
                        if (token != "") {
                            val newFragment = AddToPlaylistDialog()
                            val bundle = Bundle()
                            bundle.putString("videoId", videoId)
                            newFragment.arguments = bundle
                            newFragment.show(
                                parentFragmentManager,
                                AddToPlaylistDialog::class.java.name
                            )
                        } else {
                            Toast.makeText(context, R.string.login_first, Toast.LENGTH_SHORT).show()
                        }
                    }
                    context?.getString(R.string.download) -> {
                        val downloadDialog = DownloadDialog(videoId)
                        downloadDialog.show(parentFragmentManager, DownloadDialog::class.java.name)
                    }
                    context?.getString(R.string.share) -> {
                        val shareDialog = ShareDialog(videoId, false)
                        // using parentFragmentManager is important here
                        shareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                    }
                    context?.getString(R.string.add_to_queue) -> {
                        Globals.playingQueue += videoId
                    }
                }
            }
            .show()
    }
}
