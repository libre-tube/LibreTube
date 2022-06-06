package com.github.libretube.util

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.github.libretube.BuildConfig
import com.github.libretube.dialogs.UpdateAvailableDialog
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
    }
}

fun getUpdateInfo(): UpdateInfo? {
    val latest = URL("https://api.github.com/repos/libre-tube/LibreTube/releases/latest")
    val json = StringBuilder()
    val urlConnection: HttpsURLConnection?
    urlConnection = latest.openConnection() as HttpsURLConnection
    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))

    var line: String?
    while (br.readLine().also { line = it } != null) json.append(line)

    // Parse and return json data
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
                    Log.i("", "Lastest version: $tagName")
                    return UpdateInfo(updateUrl, tagName)
                }
            }
        }
    }
    return null
}

// data class for the update info, required to return the data
data class UpdateInfo(
    val updateUrl: String,
    val tagName: String
)
