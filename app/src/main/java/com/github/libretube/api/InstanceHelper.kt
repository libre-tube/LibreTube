package com.github.libretube.api

import android.content.Context
import com.github.libretube.R
import com.github.libretube.api.obj.Instances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstanceHelper {
    private const val PIPED_INSTANCES_URL = "https://piped-instances.kavin.rocks"
    private const val FALLBACK_INSTANCES_URL = "https://instances.tokhmi.xyz"

    /**
     * fetch official public instances from kavin.rocks as well as tokhmi.xyz as fallback
     */
    suspend fun getInstances(context: Context): List<Instances> {
        return withContext(Dispatchers.IO) {
            runCatching {
                RetrofitInstance.externalApi.getInstances(PIPED_INSTANCES_URL)
            }.getOrNull() ?: runCatching {
                RetrofitInstance.externalApi.getInstances(FALLBACK_INSTANCES_URL)
            }.getOrNull() ?: run {
                throw Exception(context.getString(R.string.failed_fetching_instances))
            }
        }
    }

    fun getInstancesFallback(context: Context): List<Instances> {
        val instanceNames = context.resources.getStringArray(R.array.instances)
        return context.resources.getStringArray(R.array.instancesValue)
            .mapIndexed { index, instanceValue ->
                Instances(instanceNames[index], instanceValue)
            }
    }
}
