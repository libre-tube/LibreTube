package com.github.libretube.api

import com.github.libretube.api.obj.Instances
import com.github.libretube.constants.GITHUB_API_URL
import com.github.libretube.obj.update.UpdateInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface ExternalApi {
    // only for fetching servers list
    @GET
    suspend fun getInstances(@Url url: String): List<Instances>

    // fetch latest version info
    @GET(GITHUB_API_URL)
    suspend fun getUpdateInfo(): UpdateInfo
}
