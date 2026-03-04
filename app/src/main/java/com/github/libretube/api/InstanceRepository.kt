package com.github.libretube.api

import com.github.libretube.api.RetrofitInstance.PIPED_API_URL
import com.github.libretube.api.obj.PipedInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl

class InstanceRepository {
    /**
     * Fetch official public instances from kavin.rocks
     */
    suspend fun getInstances(): Result<List<PipedInstance>> = withContext(Dispatchers.IO) {
        runCatching {
            RetrofitInstance.externalApi.getInstances()
        }
    }

    companion object {
        /**
         * Hardcoded list of fallback instances.
         */
        val FALLBACK_INSTANCES = listOf(PipedInstance(name = PIPED_API_URL.toHttpUrl().host, apiUrl = PIPED_API_URL));
    }
}
