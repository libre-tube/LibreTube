package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.enums.menuoption.DownloadListMenuOption
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.serializable
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.ContextHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.activities.NoInternetActivity
import com.github.libretube.ui.dialogs.DownloadExportDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PlayingQueueMode

@UnstableApi
class DownloadOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var videoId: String

    private val exportFilePicker =
        registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { outputUri ->
            if (outputUri == null) return@registerForActivityResult

            DownloadExportDialog().apply {
                arguments = Bundle().apply {
                    putString(IntentData.videoId, videoId)
                    putString(IntentData.outputUri, outputUri.toString())
                }
            }.show(requireActivity().supportFragmentManager, null)

            dismiss()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val streamItem = arguments?.parcelable<StreamItem>(IntentData.streamItem)!!
        videoId = streamItem.url!!.toID()
        val downloadTab = arguments?.serializable<DownloadTab>(IntentData.downloadTab)!!
        val playlistId = arguments?.getString(IntentData.playlistId)

        val options = mutableListOf(
            DownloadListMenuOption.PLAY_ON_BACKGROUND,
            DownloadListMenuOption.SHARE,
            DownloadListMenuOption.DELETE,
            DownloadListMenuOption.EXPORT,
        )

        // can't navigate to video while in offline activity
        if (ContextHelper.tryUnwrapActivity<NoInternetActivity>(requireContext()) == null) {
            options += DownloadListMenuOption.GO_TO_VIDEO
        }

        val isSelectedVideoCurrentlyPlaying = PlayingQueue.getCurrent()?.url?.toID() == videoId
        if (!isSelectedVideoCurrentlyPlaying && PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.OFFLINE) {
            options += DownloadListMenuOption.PLAY_NEXT
            options += DownloadListMenuOption.ADD_TO_QUEUE
        }

        setItems(options.map { it.toBottomSheetItem(::getString) }) { which ->
            val playerData = PlayerData(
                videoId,
                playlistId = playlistId,
                downloadTab = downloadTab,
                isOffline = true
            )

            when (options[which]) {
                DownloadListMenuOption.PLAY_ON_BACKGROUND -> {
                    BackgroundHelper.playOnBackground(requireContext(), playerData)
                }

                DownloadListMenuOption.GO_TO_VIDEO -> {
                    NavigationHelper.navigateVideo(requireContext(), playerData)
                }

                DownloadListMenuOption.SHARE -> {
                    val shareData = ShareData(currentVideo = videoId)
                    val bundle = bundleOf(
                        IntentData.id to videoId,
                        IntentData.shareObjectType to ShareObjectType.VIDEO,
                        IntentData.shareData to shareData
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                }

                DownloadListMenuOption.DELETE -> {
                    setFragmentResult(DELETE_DOWNLOAD_REQUEST_KEY, bundleOf())
                }

                DownloadListMenuOption.PLAY_NEXT -> {
                    PlayingQueue.addAsNext(streamItem)
                }

                DownloadListMenuOption.ADD_TO_QUEUE -> {
                    PlayingQueue.add(streamItem)
                }

                DownloadListMenuOption.EXPORT -> {
                    exportFilePicker.launch(streamItem.title)
                    // dismissing now would cause the exportFilePicker to be dropped before
                    // the file is actually chosen by the user
                    autoDismiss = false
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    companion object {
        const val DELETE_DOWNLOAD_REQUEST_KEY = "delete_download_request_key"
    }
}
