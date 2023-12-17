package com.github.libretube.api

import android.content.Context
import com.github.libretube.R
import com.github.libretube.api.obj.PipedInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstanceHelper {
    private const val PIPED_INSTANCES_URL = "https://piped-instances.kavin.rocks"

    /**
     * Fetch official public instances from kavin.rocks
     */
    suspend fun getInstances(context: Context): List<PipedInstance> {
        return withContext(Dispatchers.IO) {
            runCatching {
                RetrofitInstance.externalApi.getInstances(PIPED_INSTANCES_URL)
            }.getOrNull() ?: run {
                throw Exception(context.getString(R.string.failed_fetching_instances))
            }
        }
    }

    fun getInstancesFallback(context: Context): List<PipedInstance> {
        val instanceNames = context.resources.getStringArray(R.array.instances)
        return context.resources.getStringArray(R.array.instancesValue)
            .mapIndexed { index, instanceValue ->
                PipedInstance(instanceNames[index], instanceValue)
            }
    }
}
