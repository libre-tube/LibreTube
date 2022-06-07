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
        if (checkHost(intent)) {
            // start the main activity using the given URI as data if the host is known
            handleSendText(intent)
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

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.i(TAG, it)
            val pm: PackageManager = this.packageManager
            // startIntent(this, MainActivity::class.java doesn't work for the activity aliases needed for the logo switch option
            val intent = pm.getLaunchIntentForPackage(this.packageName)
            intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent?.data = Uri.parse(it)
            this.startActivity(intent)
            this.finishAndRemoveTask()
        }
    }
}
