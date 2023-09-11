package com.github.libretube.ui.sheets

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.obj.Download
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.ui.dialogs.ShareDialog

class DownloadOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var videoId: String
    private lateinit var uploader: String

    override fun onCreate(savedInstanceState: Bundle?) {
        videoId = arguments?.getString(IntentData.videoId)!!
        uploader = arguments?.getString(IntentData.channelName)!!

        val options = listOf(
            R.string.playOnBackground,
            R.string.go_to_video,
            R.string.share,
            R.string.delete
        ).map { getString(it) }

        setSimpleItems(options) { selectedIndex ->
            when (selectedIndex) {
                0 -> {
                    val playerIntent = Intent(requireContext(), OfflinePlayerService::class.java)
                        .putExtra(IntentData.videoId, videoId)
                    context?.stopService(playerIntent)
                    ContextCompat.startForegroundService(requireContext(), playerIntent)
                }

                1 -> {
                    NavigationHelper.navigateVideo(requireContext(), videoId = videoId)
                }

                2 -> {
                    val shareData = ShareData(currentVideo = uploader)
                    val bundle = bundleOf(
                        IntentData.id to videoId,
                        IntentData.shareObjectType to ShareObjectType.CHANNEL,
                        IntentData.shareData to shareData
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                }

                3 -> {
                    setFragmentResult(DELETE_DOWNLOAD_REQUEST_KEY, bundleOf())
                    dialog?.dismiss()
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    companion object {
        const val DELETE_DOWNLOAD_REQUEST_KEY = "delete_download_request_key"
    }
}
