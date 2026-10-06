package com.github.libretube.util

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class NewPipeDownloaderImpl : Downloader() {
    private val client = OkHttpClient.Builder()
        // Routers silently drop idle connections. The client would keep using such a dead connection
        // and every request on it would run into a timeout, so idle connections are closed early
        // and HTTP/2 connections are pinged to notice when they died.
        .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, IDLE_CONNECTION_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS))
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        for ((headerKey, headerValues) in headers) {
            requestBuilder.removeHeader(headerKey)
            for (headerValue in headerValues) {
                requestBuilder.addHeader(headerKey, headerValue)
            }
        }
        val response = executeOnFreshConnectionIfNeeded(requestBuilder.build())

        return when (response.code) {
            429 -> {
                response.close()
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            else -> {
                val responseBodyToReturn = response.body.string()
                Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    responseBodyToReturn,
                    response.request.url.toString()
                )
            }
        }
    }

    /**
     * If a request fails because of a timeout, the pooled connection it used might be dead.
     * Drop all pooled connections and try once more on a new one.
     */
    private fun executeOnFreshConnectionIfNeeded(request: okhttp3.Request): okhttp3.Response {
        return try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            client.connectionPool.evictAll()
            client.newCall(request).execute()
        }
    }

    companion object {
        private const val MAX_IDLE_CONNECTIONS = 5
        private const val IDLE_CONNECTION_KEEP_ALIVE_SECONDS = 20L
        private const val PING_INTERVAL_SECONDS = 15L

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"
    }
}