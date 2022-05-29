package com.github.libretube

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class RouterActivity : AppCompatActivity() {
    val TAG = "RouterActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    handleSendText(intent)
                } else {
                    // start app as normal if wrong intent type
                    restartMainActivity(this)
                }
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.i(TAG, it)
            val pm: PackageManager = this.packageManager
            val intent = pm.getLaunchIntentForPackage(this.packageName)
            intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent?.data = Uri.parse(it)
            this.startActivity(intent)
            this.finishAndRemoveTask()
        }
    }
}
