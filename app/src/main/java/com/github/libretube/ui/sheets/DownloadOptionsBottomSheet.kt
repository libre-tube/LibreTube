package com.github.libretube.ui.sheets

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.Download
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.services.OfflinePlayerService
import com.github.libretube.ui.dialogs.ShareDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.io.path.deleteIfExists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class DownloadOptionsBottomSheet(
    private val download: Download,
    private val items: List<DownloadItem>,
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
                    ShareDialog(download.videoId, ShareObjectType.VIDEO, shareData)
                        .show(parentFragmentManager, null)
                }

                3 -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete)
                        .setMessage(R.string.irreversible)
                        .setPositiveButton(R.string.okay) { _, _ ->
                            items.forEach {
                                it.path.deleteIfExists()
                            }
                            download.thumbnailPath?.deleteIfExists()

                            runBlocking(Dispatchers.IO) {
                                DatabaseHolder.Database.downloadDao().deleteDownload(download)
                            }
                            onDelete.invoke()
                            dialog?.dismiss()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}
