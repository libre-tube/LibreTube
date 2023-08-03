package com.github.libretube.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import com.github.libretube.constants.IntentData
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.util.TextUtils.toTimeInSeconds

class RouterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            // start processing the given text
            handleSendText(intent.getStringExtra(Intent.EXTRA_TEXT)!!.toUri())
        } else if (intent.data != null) {
            // link shared as text to the app
            handleSendText(intent.data!!)
        } else {
            // start app as normal if unknown action, shouldn't be reachable
            NavigationHelper.restartMainActivity(this)
        }
    }

    /**
     * Resolve the uri and return a bundle with the arguments
     */
    private fun Intent.resolveType(uri: Uri) = apply {
        val channelNamePaths = listOf("/c/", "/user/")
        val videoPaths = listOf("/shorts/", "/embed/", "/v/", "/live/")
        when {
            uri.path!!.contains("/results") -> {
                val searchQuery = uri.getQueryParameter("search_query")

                putExtra(IntentData.query, searchQuery)
            }

            uri.path!!.contains("/channel/") -> {
                val channelId = uri.path!!
                    .replace("/channel/", "")

                putExtra(IntentData.channelId, channelId)
            }

            channelNamePaths.any { uri.path!!.contains(it) } -> {
                var channelName = uri.path!!

                channelNamePaths.forEach {
                    channelName = channelName.replace(it, "")
                }

                putExtra(IntentData.channelName, channelName)
            }

            uri.path!!.contains("/playlist") -> {
                val playlistId = uri.getQueryParameter("list")

                putExtra(IntentData.playlistId, playlistId)
            }

            videoPaths.any { uri.path!!.contains(it) } -> {
                var videoId = uri.path!!

                videoPaths.forEach {
                    videoId = videoId.replace(it, "")
                }

                putExtra(IntentData.videoId, videoId)

                uri.getQueryParameter("t")
                    ?.let { putExtra(IntentData.timeStamp, it.toTimeInSeconds()) }
            }

            uri.path!!.contains("/watch") && uri.query != null -> {
                val videoId = uri.getQueryParameter("v")

                putExtra(IntentData.videoId, videoId)
                uri.getQueryParameter("t")
                    ?.let { putExtra(IntentData.timeStamp, it.toTimeInSeconds()) }
            }

            else -> {
                val videoId = uri.path!!.replace("/", "")

                putExtra(IntentData.videoId, videoId)
                uri.getQueryParameter("t")
                    ?.let { putExtra(IntentData.timeStamp, it.toTimeInSeconds()) }
            }
        }
    }

    private fun handleSendText(uri: Uri) {
        Log.i(TAG(), uri.toString())

        val intent = this.packageManager.getLaunchIntentForPackage(
            this.packageName
        )!!.resolveType(uri)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAndRemoveTask()
    }
}
