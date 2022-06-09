package com.github.libretube

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.util.ThemeHelper

class RouterActivity : AppCompatActivity() {
    val TAG = "RouterActivity"
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
            ThemeHelper().restartMainActivity(this)
        }
    }

    private fun checkHost(intent: Intent): Boolean {
        // check whether the host is known, current solution to replace the broken intent filter
        val hostsList = resources.getStringArray(R.array.shareHostsList)
        val intentDataUri: Uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
        val intentDataHost = intentDataUri.host
        Log.d(TAG, "$intentDataHost")
        return hostsList.contains(intentDataHost)
    }

    private fun handleSendText(uri: Uri) {
        Log.i(TAG, uri.toString())
        val pm: PackageManager = this.packageManager
        val intent = pm.getLaunchIntentForPackage(this.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent?.data = uri
        this.startActivity(intent)
        this.finishAndRemoveTask()
    }
}
