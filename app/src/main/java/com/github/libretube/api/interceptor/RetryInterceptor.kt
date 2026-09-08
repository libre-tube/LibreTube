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
 * - 5xx/429 responses to non-idempotent methods (e.g. POST): the server may have already applied
 *   the mutation before failing, so a blind retry could duplicate its side effects.
 */
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val initialDelayMs: Long = INITIAL_DELAY_MS,
    private val maxDelayMs: Long = MAX_DELAY_MS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method

        for (attempt in 0..maxRetries) {
            val response: Response = try {
                chain.proceed(request)
            } catch (e: IOException) {
                // Network failure: the request likely never reached the server, so retrying is safe
                // for every method.
                if (attempt == maxRetries) {
                    throw e
                }
                val delayMs = calculateDelay(attempt)
                Log.w(TAG, "Network error: ${e.message}, retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                sleep(delayMs)
                continue
            }

            // Success or non-retryable status: hand the response to the caller.
            if (response.isSuccessful || !isRetryable(response.code)) {
                return response
            }

            // Never blindly retry requests with side effects: the server may have applied the
            // mutation before answering 5xx/429 and a retry would duplicate it.
            if (method !in IDEMPOTENT_METHODS) {
                Log.w(TAG, "Not retrying HTTP ${response.code} for non-idempotent $method request")
                return response
            }

            // Last attempt already used: hand the real status code back instead of sleeping once
            // more and then synthesizing an error.
            if (attempt == maxRetries) {
                return response
            }

            val retryAfter = response.header("Retry-After")?.toLongOrNull()
            val delayMs = if (response.code == HTTP_TOO_MANY_REQUESTS) {
                // 429 Rate Limited - respect Retry-After header, capped so a prolonged outage or a
                // malicious value cannot pin an OkHttp dispatcher thread down forever
                retryAfter?.let { min(it * 1000, maxDelayMs) } ?: calculateDelay(attempt)
            } else {
                calculateDelay(attempt)
            }
            Log.w(TAG, "Retryable HTTP ${response.code}, retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
            // Close the response body so the connection is released before we sleep
            closeResponse(response)
            sleep(delayMs)
        }

        throw IOException("All $maxRetries retries exhausted")
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
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
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

        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE")
    }
}