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

    suspend fun getInstancesFallback(): List<PipedInstance> = withContext(Dispatchers.IO) {
        return@withContext try {
            RetrofitInstance.externalApi.getInstancesMarkdown().body()!!.string().lines().reversed()
                .takeWhile { !it.startsWith("---") }
                .filter { it.isNotBlank() }
                .map { line ->
                    val infoParts = line.split("|").map { it.trim() }

                    PipedInstance(name = infoParts[0], apiUrl = infoParts[1], locations = infoParts[2], cdn = infoParts[3] == "Yes")
                }
        } catch (e: Exception) {
            // worst case scenario: only return official instance
            return@withContext listOf(PipedInstance(name = PIPED_API_URL.toHttpUrl().host, apiUrl = PIPED_API_URL))
        }
    }
}
