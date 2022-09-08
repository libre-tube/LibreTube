package com.github.libretube.update

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.constants.GITHUB_API_URL
import com.github.libretube.extensions.await
import java.net.URL

object UpdateChecker {
    fun getLatestReleaseInfo(): UpdateInfo? {
        var versionInfo: UpdateInfo? = null
        // run http request as thread to make it async
        Thread {
            // otherwise crashes without internet
            versionInfo = getUpdateInfo()
            try {
                versionInfo = getUpdateInfo()
            } catch (e: Exception) {
            }
        }.await()

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
