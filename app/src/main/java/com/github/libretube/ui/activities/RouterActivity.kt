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
        val uri = intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri() ?: intent.data
        if (uri != null) {
            // Start processing the given text, if available. Otherwise use the link shared as text
            // to the app.
            handleSendText(uri)
        } else {
            // start app as normal if unknown action, shouldn't be reachable
            NavigationHelper.restartMainActivity(this)
        }
    }

    /**
     * Resolve the uri and return a bundle with the arguments
     */
    private fun Intent.resolveType(uri: Uri) = apply {
        val lastSegment = uri.lastPathSegment
        val secondLastSegment = uri.pathSegments.getOrNull(uri.pathSegments.size - 2)
        when {
            lastSegment == "results" -> {
                putExtra(IntentData.query, uri.getQueryParameter("search_query"))
            }
            secondLastSegment == "channel" -> {
                putExtra(IntentData.channelId, lastSegment)
            }
            secondLastSegment == "c" || secondLastSegment == "user" -> {
                putExtra(IntentData.channelName, lastSegment)
            }
            lastSegment == "playlist" -> {
                putExtra(IntentData.playlistId, uri.getQueryParameter("list"))
            }
            else -> {
                val id = if (lastSegment == "watch") uri.getQueryParameter("v") else lastSegment
                putExtra(IntentData.videoId, id)
                putExtra(IntentData.timeStamp, uri.getQueryParameter("t")?.toTimeInSeconds())
            }
        }
    }

    private fun handleSendText(uri: Uri) {
        Log.i(TAG(), uri.toString())

        val intent = packageManager.getLaunchIntentForPackage(packageName)!!.resolveType(uri)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAndRemoveTask()
    }
}
