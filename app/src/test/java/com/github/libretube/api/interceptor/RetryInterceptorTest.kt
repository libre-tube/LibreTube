package com.github.libretube.api.interceptor

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class RetryInterceptorTest {

    private fun request(method: String = "GET"): Request {
        return Request.Builder()
            .url("https://example.com/api")
            .method(method, if (method == "GET") null else ByteArray(0).toRequestBody())
            .build()
    }

    /** Fake chain returning a scripted sequence of responses or exceptions. */
    private class FakeChain(
        private val request: Request,
        private val responses: Array<out Any>,
    ) : Interceptor.Chain {
        var callCount = 0
            private set

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            val next = responses[callCount.coerceAtMost(responses.lastIndex)]
            callCount++
            if (next is IOException) {
                throw next
            }
            if (next is Response) {
                return next
            }
            val code = next as Int
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("msg")
                .body(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 10_000
        override fun readTimeoutMillis(): Int = 10_000
        override fun writeTimeoutMillis(): Int = 10_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private fun fastInterceptor(maxRetries: Int = 2, maxDelayMs: Long = 50): RetryInterceptor {
        return RetryInterceptor(maxRetries = maxRetries, initialDelayMs = 1, maxDelayMs = maxDelayMs)
    }

    @Test
    fun `successful response is not retried`() {
        val chain = FakeChain(request(), arrayOf(200))
        val response = fastInterceptor().intercept(chain)
        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `transient 500 is retried until success`() {
        val chain = FakeChain(request(), arrayOf(500, 200))
        val response = fastInterceptor().intercept(chain)
        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
    }

    @Test
    fun `multiple transient errors are retried`() {
        val chain = FakeChain(request(), arrayOf(502, 503, 200))
        val response = fastInterceptor(maxRetries = 2).intercept(chain)
        assertEquals(200, response.code)
        assertEquals(3, chain.callCount)
    }

    @Test
    fun `retries are exhausted after maxRetries`() {
        val chain = FakeChain(request(), arrayOf(500))
        val response = fastInterceptor(maxRetries = 2).intercept(chain)
        // the real HTTP status must be handed back, not a synthetic error
        assertEquals(500, response.code)
        assertEquals(3, chain.callCount)
    }

    @Test
    fun `404 is not retried`() {
        val chain = FakeChain(request(), arrayOf(404))
        val response = fastInterceptor().intercept(chain)
        assertEquals(404, response.code)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `501 is not retried`() {
        val chain = FakeChain(request(), arrayOf(501))
        val response = fastInterceptor().intercept(chain)
        assertEquals(501, response.code)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `429 is retried`() {
        val chain = FakeChain(request(), arrayOf(429, 200))
        val response = fastInterceptor().intercept(chain)
        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
    }

    @Test
    fun `retry-after delay is capped at maxDelay`() {
        // a response carrying Retry-After: 100 (== 100s) with a max delay of 50ms must only sleep ~50ms
        val rateLimited = Response.Builder()
            .request(request())
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .header("Retry-After", "100")
            .body(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))
            .build()
        val chain = FakeChain(request(), arrayOf(rateLimited, 200))
        val start = System.nanoTime()
        val response = fastInterceptor(maxRetries = 2, maxDelayMs = 50).intercept(chain)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertEquals(200, response.code)
        // the uncapped delay would be 100000ms; a cap of 50ms must keep the total well under 2s
        assertEquals(true, elapsedMs < 2_000)
    }

    @Test
    fun `post on 500 is not retried (idempotency)`() {
        val chain = FakeChain(request(method = "POST"), arrayOf(500))
        val response = fastInterceptor().intercept(chain)
        assertEquals(500, response.code)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `post on 429 is not retried (idempotency)`() {
        val chain = FakeChain(request(method = "POST"), arrayOf(429))
        val response = fastInterceptor().intercept(chain)
        assertEquals(429, response.code)
        assertEquals(1, chain.callCount)
    }

    @Test
    fun `ioexception is retried for post requests`() {
        val chain = FakeChain(request(method = "POST"), arrayOf(SocketTimeoutException("timeout"), 200))
        val response = fastInterceptor().intercept(chain)
        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
    }

    @Test
    fun `ioexception is retried`() {
        val chain = FakeChain(request(), arrayOf(IOException("boom"), 200))
        val response = fastInterceptor().intercept(chain)
        assertEquals(200, response.code)
        assertEquals(2, chain.callCount)
    }

    @Test
    fun `ioexception after retries is rethrown`() {
        val chain = FakeChain(request(), arrayOf(SocketTimeoutException("timeout")))
        assertThrows(SocketTimeoutException::class.java) {
            fastInterceptor(maxRetries = 1).intercept(chain)
        }
        assertEquals(2, chain.callCount)
    }
}