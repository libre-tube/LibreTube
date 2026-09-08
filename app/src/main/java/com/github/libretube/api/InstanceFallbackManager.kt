package com.github.libretube.api

import android.util.Log
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages automatic fallback between Piped instances when the primary instance fails.
 *
 * When a streaming or API request fails, this manager:
 * 1. Marks the failed instance as potentially down
 * 2. Tries alternative instances from the user's custom instance list
 * 3. Returns the first working instance
 * 4. Updates the preference to use the working instance for future requests
 */
object InstanceFallbackManager {
    private const val TAG = "InstanceFallback"
    private const val HEALTH_CHECK_TIMEOUT_SECONDS = 8L
    private const val MAX_FALLBACK_ATTEMPTS = 3
    private const val INSTANCE_COOLDOWN_MS = 60_000L // 1 minute cooldown before retrying a failed instance

    private data class InstanceHealth(
        val apiUrl: String,
        var lastFailureTime: Long = 0L,
        var failureCount: Int = 0,
    )

    private val instanceHealthMap = ConcurrentHashMap<String, InstanceHealth>()

    private val healthCheckClient = OkHttpClient.Builder()
        .connectTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Mark the current instance as failed and switch to a fallback if available.
     * Returns the new instance URL, or null if no fallback was found.
     */
    suspend fun onInstanceFailed(failedApiUrl: String): String? = withContext(Dispatchers.IO) {
        markInstanceFailed(failedApiUrl)
        findWorkingInstance(failedApiUrl)
    }

    private fun markInstanceFailed(apiUrl: String) {
        // atomic read-modify-write: concurrent failure notifications must not lose an update
        instanceHealthMap.compute(apiUrl) { _, existing ->
            val health = existing ?: InstanceHealth(apiUrl)
            health.lastFailureTime = System.currentTimeMillis()
            health.failureCount++
            health
        }?.let { Log.w(TAG, "Instance $apiUrl marked as failed (failures: ${it.failureCount})") }
    }

    /**
     * Pure candidate-selection logic: an instance in cooldown (one that failed less than
     * [INSTANCE_COOLDOWN_MS] ago) is not eligible as a fallback.
     */
    internal fun isInstanceInCooldown(
        lastFailureTime: Long,
        nowMs: Long,
        cooldownMs: Long = INSTANCE_COOLDOWN_MS,
    ): Boolean {
        if (lastFailureTime == 0L) return false
        return nowMs - lastFailureTime < cooldownMs
    }

    /**
     * Pure fallback-candidate selection, preserving the user-configured order:
     * excludes the failed instance, excludes instances still in cooldown and caps the number of
     * candidates to health-check. Kept side-effect free so it can be unit-tested.
     */
    internal fun selectFallbackCandidates(
        apiUrls: List<String>,
        excludeApiUrl: String,
        lastFailureTimes: Map<String, Long>,
        nowMs: Long,
        maxCandidates: Int = MAX_FALLBACK_ATTEMPTS,
    ): List<String> = apiUrls
        .filter { it != excludeApiUrl }
        .filter { !isInstanceInCooldown(lastFailureTimes[it] ?: 0L, nowMs) }
        .take(maxCandidates)

    /**
     * Find a working instance from the list of custom instances,
     * excluding the failed instance.
     */
    private suspend fun findWorkingInstance(excludeApiUrl: String): String? {
        val customInstances = DatabaseHolder.Database.customInstanceDao().getAll()

        // Filter out the failed instance and instances in cooldown
        val nowMs = System.currentTimeMillis()
        val candidates = selectFallbackCandidates(
            apiUrls = customInstances.map { it.apiUrl },
            excludeApiUrl = excludeApiUrl,
            lastFailureTimes = instanceHealthMap.mapValues { it.value.lastFailureTime },
            nowMs = nowMs,
        )

        if (candidates.isEmpty()) {
            Log.e(TAG, "No fallback candidates available")
            return null
        }

        // health-check all candidates concurrently to keep the instance-switch latency bounded
        return coroutineScope {
            val reachable = candidates.map { apiUrl ->
                async(Dispatchers.IO) { apiUrl to isInstanceReachable(apiUrl) }
            }.awaitAll()

            reachable.firstOrNull { it.second }?.first?.also { workingApiUrl ->
                Log.i(TAG, "Found working fallback instance: $workingApiUrl")
                // Update the preference to use this instance
                PreferenceHelper.putString(PreferenceKeys.FETCH_INSTANCE, workingApiUrl)
                RetrofitInstance.apiLazyMgr.reset()
            } ?: run {
                candidates.forEach { markInstanceFailed(it) }
                Log.e(TAG, "No working fallback instance found")
                null
            }
        }
    }

    /**
     * Check if a Piped instance is reachable by hitting its /config endpoint.
     */
    private fun isInstanceReachable(apiUrl: String): Boolean {
        return try {
            val url = "${apiUrl.trimEnd('/')}/config"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val response = healthCheckClient.newCall(request).execute()
            val isReachable = response.isSuccessful
            val responseCode = response.code
            response.close()
            Log.d(TAG, "Health check for $apiUrl: ${if (isReachable) "OK" else "FAIL ($responseCode)"}")
            isReachable
        } catch (e: kotlinx.coroutines.CancellationException) {
            // propagate cancellation instead of swallowing it as a failed health check
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed for $apiUrl: ${e.message}")
            false
        }
    }
}
