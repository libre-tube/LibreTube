package com.github.libretube.api

import com.github.libretube.constants.GITHUB_API_URL
import com.github.libretube.constants.PIPED_INSTANCES_URL
import com.github.libretube.obj.Instances
import com.github.libretube.update.UpdateInfo
import retrofit2.http.GET

interface ExternalApi {
    // only for fetching servers list
    @GET(PIPED_INSTANCES_URL)
    suspend fun getInstances(): List<Instances>

    // fetch latest version info
    @GET(GITHUB_API_URL)
    suspend fun getUpdateInfo(): UpdateInfo
}
