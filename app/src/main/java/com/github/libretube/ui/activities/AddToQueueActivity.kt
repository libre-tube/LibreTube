package com.github.libretube.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.github.libretube.constants.IntentData
import com.github.libretube.helpers.IntentHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.util.PlayingQueue

/**
 * Receives a text by the intent and attempts to add it to the playing queue
 * If no video is playing currently, the queue will be left unchanged and the the main activity is being resumed
 */
class AddToQueueActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoId = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { IntentHelper.resolveType(Intent(), it.toUri()) }
            ?.getStringExtra(IntentData.videoId)

        if (videoId != null) {
            val newIntent = packageManager.getLaunchIntentForPackage(packageName)

            // if playing a video currently, the video will be added to the queue
            if (PlayingQueue.isNotEmpty()) {
                PlayingQueue.insertByVideoId(videoId)
            } else {
                newIntent?.putExtra(IntentData.videoId, videoId)
            }

            startActivity(newIntent)
        }

        finishAndRemoveTask()
    }
}
