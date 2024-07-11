package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.github.libretube.constants.IntentData
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog

class AddToPlaylistActivity: BaseActivity() {
    override val isDialogActivity: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoId = intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            IntentHelper.resolveType(Intent(), it.toUri())
        }?.getStringExtra(IntentData.videoId)

        if (videoId == null) {
            finishAndRemoveTask()
            return
        }

        supportFragmentManager.setFragmentResultListener(
            AddToPlaylistDialog.ADD_TO_PLAYLIST_DIALOG_DISMISSED_KEY,
            this
        ) { _, _ -> finish() }

        AddToPlaylistDialog().apply {
            arguments = bundleOf(IntentData.videoId to videoId)
        }.show(supportFragmentManager, null)
    }
}