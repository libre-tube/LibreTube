package com.github.libretube.ui.sheets

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.obj.Download
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.ui.dialogs.ShareDialog

class DownloadOptionsBottomSheet(
    private val download: Download,
    private val onDelete: () -> Unit
) : BaseBottomSheet() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
                        .putExtra(IntentData.videoId, download.videoId)
                    context?.stopService(playerIntent)
                    ContextCompat.startForegroundService(requireContext(), playerIntent)
                }

                1 -> {
                    NavigationHelper.navigateVideo(requireContext(), videoId = download.videoId)
                }

                2 -> {
                    val shareData = ShareData(currentVideo = download.uploader)
                    val bundle = bundleOf(
                        IntentData.id to download.videoId,
                        IntentData.shareObjectType to ShareObjectType.CHANNEL,
                        IntentData.shareData to shareData
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                }

                3 -> {
                    onDelete.invoke()
                    dialog?.dismiss()
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}
