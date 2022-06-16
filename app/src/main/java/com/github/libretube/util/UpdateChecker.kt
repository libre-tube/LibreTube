package com.github.libretube.util

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.github.libretube.BuildConfig
import com.github.libretube.GITHUB_API_URL
import com.github.libretube.dialogs.NoUpdateAvailableDialog
import com.github.libretube.dialogs.UpdateAvailableDialog
import com.github.libretube.obj.UpdateInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONArray
import org.json.JSONObject

fun checkUpdate(childFragmentManager: FragmentManager) {
    var updateInfo: UpdateInfo? = UpdateInfo("", "")
    // run http request as thread to make it async
    val thread = Thread {
        // otherwise crashes without internet
        try {
            updateInfo = getUpdateInfo()
        } catch (e: Exception) {
        }
    }
    thread.start()
    // wait for the thread to finish
    thread.join()
    // show the UpdateAvailableDialog if there's an update available
    if (updateInfo?.tagName != "" && BuildConfig.VERSION_NAME != updateInfo?.tagName) {
        val updateAvailableDialog = UpdateAvailableDialog(
            updateInfo?.tagName!!,
            updateInfo?.updateUrl!!
        )
        updateAvailableDialog.show(childFragmentManager, "UpdateAvailableDialog")
    } else {
        // otherwise show the no update available dialog
        val noUpdateAvailableDialog = NoUpdateAvailableDialog()
        noUpdateAvailableDialog.show(childFragmentManager, "NoUpdateAvailableDialog")
    }
}

fun getUpdateInfo(): UpdateInfo? {
    val latest = URL(GITHUB_API_URL)
    val json = StringBuilder()
    val urlConnection: HttpsURLConnection?
    urlConnection = latest.openConnection() as HttpsURLConnection
    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))

    var line: String?
    while (br.readLine().also { line = it } != null) json.append(line)

    // Parse and return the json data
    val jsonRoot = JSONObject(json.toString())
    if (jsonRoot.has("tag_name") &&
        jsonRoot.has("html_url") &&
        jsonRoot.has("assets")
    ) {
        val updateUrl = jsonRoot.getString("html_url")
        val jsonAssets: JSONArray = jsonRoot.getJSONArray("assets")
        for (i in 0 until jsonAssets.length()) {
            val jsonAsset = jsonAssets.getJSONObject(i)
            if (jsonAsset.has("name")) {
                val name = jsonAsset.getString("name")
                if (name.endsWith(".apk")) {
                    val tagName = jsonRoot.getString("name")
                    Log.i("", "Latest version: $tagName")
                    return UpdateInfo(updateUrl, tagName)
                }
            }
        }
    }
    return null
}
