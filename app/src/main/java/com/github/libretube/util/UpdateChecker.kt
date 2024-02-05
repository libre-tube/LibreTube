package com.github.libretube.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.dialogs.UpdateAvailableDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateChecker(private val context: Context) {
    private lateinit var changelog: String
    private lateinit var releaseURL: String

    suspend fun checkUpdate(manualTrigger: Boolean = false) {
        val currentAppVersion = BuildConfig.VERSION_NAME.replace(".", "")

        try {
            val response = RetrofitInstance.externalApi.getLatestRelease()

            if (response.isSuccessful) {
                val latestRelease = response.body()
                releaseURL = latestRelease?.html_url.toString()

                // version would be in the format "0.21.1"
                val update = latestRelease?.name?.replace(".", "")?.toIntOrNull()
                changelog = latestRelease?.body.toString()

                if (update != null && currentAppVersion.toInt() < update) {
                    withContext(Dispatchers.Main) {
                        UpdateAvailableDialog().showDialog(
                            sanitizeChangelog(changelog),
                            releaseURL,
                            context,
                        )
                    }
                } else if (manualTrigger) {
                    Toast.makeText(context, R.string.app_uptodate, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show()
                Log.e(TAG(), response.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeChangelog(changelog: String): String {
        val sanitizedChangelog = changelog.substringBeforeLast("**Full Changelog**")
        val withoutLinks = sanitizedChangelog.replace(Regex("in https://github\\.com/[^\\s]+"), "")

        return withoutLinks.trim()
    }
}
