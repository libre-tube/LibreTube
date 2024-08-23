package com.github.libretube.util

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

class NewPipeDownloaderImpl : Downloader() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val url = request.url()

        val requestBody = request.dataToSend()?.let {
            it.toRequestBody(APPLICATION_JSON, 0, it.size)
        }

        val requestBuilder = okhttp3.Request.Builder()
            .method(request.httpMethod(), requestBody)
            .url(url)
            .addHeader(USER_AGENT_HEADER_NAME, USER_AGENT)

        for ((headerName, headerValueList) in request.headers()) {
            requestBuilder.removeHeader(headerName)
            for (headerValue in headerValueList) {
                requestBuilder.addHeader(headerName, headerValue)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (response.code == CAPTCHA_STATUS_CODE) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            response.request.url.toString()
        )
    }

    companion object {
        private const val USER_AGENT_HEADER_NAME = "User-Agent"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:102.0) Gecko/20100101 Firefox/102.0"
        private const val CAPTCHA_STATUS_CODE = 429
        private val APPLICATION_JSON = "application/json".toMediaType()
        private const val READ_TIMEOUT_SECONDS = 30L
    }
}