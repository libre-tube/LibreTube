package com.github.libretube.ui.sheets

import android.os.Bundle
import android.util.Log
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.ShareDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [channelId] to load the content from the right video.
 */
class ChannelOptionsBottomSheet(
    private val channelId: String,
    channelName: String?
) : BaseBottomSheet() {
    private val shareData = ShareData(currentChannel = channelName)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            getString(R.string.share),
            getString(R.string.play_latest_videos),
            getString(R.string.playOnBackground)
        )

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                getString(R.string.share) -> {
                    val bundle = Bundle().apply {
                        putString("id", channelId)
                        putSerializable("shareObjectType", ShareObjectType.CHANNEL)
                        putParcelable("shareData", shareData)
                    }
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    newShareDialog.show(parentFragmentManager, null)
                }

                getString(R.string.play_latest_videos) -> {
                    try {
                        val channel = withContext(Dispatchers.IO) {
                            RetrofitInstance.api.getChannel(channelId)
                        }
                        channel.relatedStreams.firstOrNull()?.url?.toID()?.let {
                            NavigationHelper.navigateVideo(
                                requireContext(),
                                it,
                                channelId = channelId
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG(), e.toString())
                    }
                }

                getString(R.string.playOnBackground) -> {
                    try {
                        val channel = withContext(Dispatchers.IO) {
                            RetrofitInstance.api.getChannel(channelId)
                        }
                        channel.relatedStreams.firstOrNull()?.url?.toID()?.let {
                            BackgroundHelper.playOnBackground(
                                requireContext(),
                                videoId = it,
                                channelId = channelId
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG(), e.toString())
                    }
                }
            }
        }
    }
}
