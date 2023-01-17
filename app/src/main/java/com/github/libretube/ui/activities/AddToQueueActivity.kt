package com.github.libretube.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.util.PlayingQueue

/**
 * Receives a text by the intent and attempts to add it to the playing queue
 * If no video is playing currently, the queue will be left unchanged and the the main activity is being resumed
 */
class AddToQueueActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
        var videoId: String? = null
        listOf("/shorts/", "/v/", "/embed/").forEach {
            if (uri.path!!.contains(it)) {
                videoId = uri.path!!.replace(it, "")
            }
        }
        if (
            uri.path!!.contains("/watch") && uri.query != null
        ) {
            videoId = uri.getQueryParameter("v")
        }

        if (videoId == null) videoId = uri.path!!.replace("/", "")

        // if playing a video currently, the playing queue is not empty
        if (PlayingQueue.isNotEmpty()) PlayingQueue.insertByVideoId(videoId!!)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(intent)
        finishAndRemoveTask()
    }
}
