package com.github.libretube.ui.sheets

import android.os.Bundle
import com.github.libretube.R
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.dialogs.ShareDialog

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class ChannelOptionsBottomSheet(
    private val channelId: String,
    private val channelName: String?
) : BaseBottomSheet() {
    private val shareData = ShareData(currentChannel = channelName)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            context?.getString(R.string.share)!!
        )

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                getString(R.string.share) -> {
                    ShareDialog(channelId, ShareObjectType.CHANNEL, shareData)
                        .show(parentFragmentManager, null)
                }
            }
        }
    }
}
