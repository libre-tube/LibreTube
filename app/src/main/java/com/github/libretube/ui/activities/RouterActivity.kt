package com.github.libretube.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.util.NavigationHelper

class RouterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getStringExtra(Intent.EXTRA_TEXT) != null && checkHost(intent)) {
            // start the main activity using the given URI as data if the host is known
            val uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
            handleSendText(uri)
        } else if (intent.data != null) {
            val uri = intent.data
            handleSendText(uri!!)
        } else {
            // start app as normal if URI not in host list
            NavigationHelper.restartMainActivity(this)
        }
    }

    private fun checkHost(intent: Intent): Boolean {
        // check whether the host is known, current solution to replace the broken intent filter
        val hostsList = resources.getStringArray(R.array.shareHostsList)
        val intentDataUri: Uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
        val intentDataHost = intentDataUri.host
        Log.d(TAG(), "$intentDataHost")
        return hostsList.contains(intentDataHost)
    }

    /**
     * Resolve the uri and return a bundle with the arguments
     */
    private fun resolveType(intent: Intent, uri: Uri): Intent {
        when {
            uri.path!!.contains("/channel/") -> {
                val channelId = uri.path!!
                    .replace("/channel/", "")

                intent.putExtra(IntentData.channelId, channelId)
            }
            uri.path!!.contains("/c/") || uri.path!!.contains("/user/") -> {
                val channelName = uri.path!!
                    .replace("/c/", "")
                    .replace("/user/", "")

                intent.putExtra(IntentData.channelName, channelName)
            }
            uri.path!!.contains("/playlist") -> {
                val playlistId = uri.getQueryParameter("list")

                intent.putExtra(IntentData.playlistId, playlistId)
            }
            uri.path!!.contains("/shorts/") ||
                uri.path!!.contains("/embed/") ||
                uri.path!!.contains("/v/")
            -> {
                val videoId = uri.path!!
                    .replace("/shorts/", "")
                    .replace("/v/", "")
                    .replace("/embed/", "")

                intent.putExtra(IntentData.videoId, videoId)
            }
            uri.path!!.contains("/watch") && uri.query != null -> {
                val videoId = uri.getQueryParameter("v")

                intent.putExtra(IntentData.videoId, videoId)
                uri.getQueryParameter("t")
                    ?.let { intent.putExtra(IntentData.timeStamp, it.toLong()) }
            }
            else -> {
                val videoId = uri.path!!.replace("/", "")

                intent.putExtra(IntentData.videoId, videoId)
                uri.getQueryParameter("t")
                    ?.let { intent.putExtra(IntentData.timeStamp, it.toLong()) }
            }
        }
        return intent
    }

    private fun handleSendText(uri: Uri) {
        Log.i(TAG(), uri.toString())

        val pm: PackageManager = this.packageManager
        val intent = pm.getLaunchIntentForPackage(this.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(
            resolveType(intent!!, uri)
        )
        this.finishAndRemoveTask()
    }
}
