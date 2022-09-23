package com.github.libretube.sheets

import android.os.Bundle
import android.widget.Toast
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.views.BottomSheet
import com.github.libretube.util.BackgroundHelper
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PreferenceHelper

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsBottomSheet(
    private val videoId: String
) : BottomSheet() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            context?.getString(R.string.playOnBackground)!!,
            context?.getString(R.string.addToPlaylist)!!,
            context?.getString(R.string.download)!!,
            context?.getString(R.string.share)!!
        )

        // remove the add to playlist option if not logged in
        if (PreferenceHelper.getToken() == "") {
            optionsList.remove(
                context?.getString(R.string.addToPlaylist)

            )
        }

        /**
         * Check whether the player is running and add queue options
         */
        if (PlayingQueue.isNotEmpty()) {
            optionsList += context?.getString(R.string.play_next)!!
            optionsList += context?.getString(R.string.add_to_queue)!!
        }

        setSimpleItems(optionsList) { which ->
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
                        bundle.putString(IntentData.videoId, videoId)
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
                context?.getString(R.string.play_next) -> {
                    PlayingQueue.addAsNext(videoId)
                }
                context?.getString(R.string.add_to_queue) -> {
                    PlayingQueue.add(videoId)
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}
