package com.github.libretube.update

import com.github.libretube.GITHUB_API_URL
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

fun checkUpdate(): UpdateInfo? {
    var versionInfo: UpdateInfo? = null
    // run http request as thread to make it async
    val thread = Thread {
        // otherwise crashes without internet
        try {
            versionInfo = getUpdateInfo()
        } catch (e: Exception) {
        }
    }
    thread.start()
    // wait for the thread to finish
    thread.join()

    // return the information about the latest version
    return versionInfo
}

fun getUpdateInfo(): UpdateInfo? {
    val latest = URL(GITHUB_API_URL)
    val json = StringBuilder()
    val urlConnection: HttpsURLConnection?
    urlConnection = latest.openConnection() as HttpsURLConnection

    // read json
    val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
    var line: String?
    while (br.readLine().also { line = it } != null) json.append(line)

    // Parse and return the json data
    val gson = Gson()
    return gson.fromJson(json.toString(), UpdateInfo::class.java)
}
