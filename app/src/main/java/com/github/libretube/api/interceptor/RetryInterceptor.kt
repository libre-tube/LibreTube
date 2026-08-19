package com.github.libretube.api.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.min

/**
 * OkHttp interceptor that retries failed requests with exponential backoff.
 *
 * Retries on:
 * - IOExceptions (network failures, timeouts)
 * - HTTP 5xx server errors (except 501 Not Implemented)
 * - HTTP 429 (Too Many Requests / Rate Limited)
 *
 * Does NOT retry on:
 * - HTTP 4xx client errors (except 429)
 * - Successful responses (2xx, 3xx)
 */
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val initialDelayMs: Long = INITIAL_DELAY_MS,
    private val maxDelayMs: Long = MAX_DELAY_MS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        for (attempt in 0..maxRetries) {
            try {
                // Close previous response body before retrying
                lastResponse?.close()

                val response = chain.proceed(request)

                // Check if response is retryable
                if (response.isSuccessful || !isRetryable(response.code)) {
                    return response
                }

                // 429 Rate Limited - respect Retry-After header
                if (response.code == HTTP_TOO_MANY_REQUESTS) {
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()
                    val delayMs = retryAfter?.let { it * 1000 } ?: calculateDelay(attempt)
                    Log.w(TAG, "Rate limited (429), retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                    closeResponse(response)
                    sleep(delayMs)
                    lastResponse = null
                    continue
                }

                // 5xx Server Error
                if (response.code in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX && response.code != HTTP_NOT_IMPLEMENTED) {
                    val delayMs = calculateDelay(attempt)
                    Log.w(TAG, "Server error (${response.code}), retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                    closeResponse(response)
                    sleep(delayMs)
                    lastResponse = null
                    continue
                }

                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = calculateDelay(attempt)
                    Log.w(TAG, "Network error: ${e.message}, retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                    sleep(delayMs)
                }
            }
        }

        // All retries exhausted
        lastResponse?.let { return it }
        throw lastException ?: IOException("All $maxRetries retries exhausted")
    }

    private fun isRetryable(code: Int): Boolean {
        return code == HTTP_TOO_MANY_REQUESTS ||
                (code in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX && code != HTTP_NOT_IMPLEMENTED)
    }

    private fun calculateDelay(attempt: Int): Long {
        // Exponential backoff with jitter
        val exponentialDelay = initialDelayMs * (1L shl attempt)
        val jitter = (Math.random() * initialDelayMs * 0.5).toLong()
        return min(exponentialDelay + jitter, maxDelayMs)
    }

    private fun closeResponse(response: Response) {
        runCatching { response.close() }
    }

    private fun sleep(ms: Long) {
        runCatching { Thread.sleep(ms) }
    }

    companion object {
        private const val TAG = "RetryInterceptor"
        private const val DEFAULT_MAX_RETRIES = 2
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_NOT_IMPLEMENTED = 501
        private const val HTTP_SERVER_ERROR_MIN = 500
        private const val HTTP_SERVER_ERROR_MAX = 599
    }
}
