package com.github.libretube.update

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.GITHUB_API_URL
import java.net.URL

object UpdateChecker {
    fun getLatestReleaseInfo(): UpdateInfo? {
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

    private fun getUpdateInfo(): UpdateInfo? {
        // get the github API response
        val latestVersionApiUrl = URL(GITHUB_API_URL)
        val json = latestVersionApiUrl.readText()

        // Parse and return the json data
        val mapper = ObjectMapper()
        return mapper.readValue(json, UpdateInfo::class.java)
    }
}
