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
import java.util.Locale

class UpdateChecker(private val context: Context) {

    suspend fun checkUpdate(isManualCheck: Boolean = false) {
        val changelog: String?
        val releaseURL: String?
        val currentAppVersion = BuildConfig.VERSION_NAME.replace(".", "").toInt()

        try {
            val response = RetrofitInstance.externalApi.getLatestRelease()
            releaseURL = response.htmlUrl

            // version would be in the format "0.21.1"
            val update = response.name.replace(".", "").toIntOrNull()
            changelog = response.body

            if (update != null && currentAppVersion < update) {
                withContext(Dispatchers.Main) {
                    UpdateAvailableDialog().onCreateDialog(
                        sanitizeChangelog(changelog),
                        releaseURL,
                        context,
                    )
                }
                Log.i(TAG(), response.toString())
            } else if (isManualCheck) {
                Toast.makeText(context, R.string.app_uptodate, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeChangelog(changelog: String): String {
        val removeBloat = changelog.substringBeforeLast("**Full Changelog**")
        val removeLinks = removeBloat.replace(Regex("in https://github\\.com/\\S+"), "")
        val uppercaseChangeType = removeLinks.lines().joinToString("\n") { line ->
            if (line.startsWith("##")) line.uppercase(Locale.ROOT) + " :" else line
        }
        val removeHashes = uppercaseChangeType.replace("## ", "")
        val cleanPrefix = removeHashes.replace("*", "•")

        return cleanPrefix.trim()
    }
}
