package com.github.libretube.api

import com.github.libretube.BuildConfig
import com.github.libretube.api.interceptor.RetryInterceptor
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    const val PIPED_API_URL = "https://pipedapi.kavin.rocks"

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 15L

    val authUrl
        get() = if (
            PreferenceHelper.getBoolean(
                PreferenceKeys.AUTH_INSTANCE_TOGGLE,
                false
            )
        ) {
           PreferenceHelper.getString(
                PreferenceKeys.AUTH_INSTANCE,
                PIPED_API_URL
            )
        } else {
            PipedMediaServiceRepository.apiUrl
        }

    val apiLazyMgr = resettableManager()
    val kotlinxConverterFactory = JsonHelper.json
        .asConverterFactory("application/json".toMediaType())

    val httpClient by lazy { buildClient() }

    val authApi by resettableLazy(apiLazyMgr) {
        buildRetrofitInstance<PipedAuthApi>(authUrl)
    }

    // the url provided here isn't actually used anywhere in the external api
    val externalApi = buildRetrofitInstance<ExternalApi>(PIPED_API_URL)

    private fun buildClient(): OkHttpClient {
        val httpClient = OkHttpClient().newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor())

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            httpClient.addInterceptor(loggingInterceptor)
        }

        return httpClient.build()
    }

    inline fun <reified T: Any> buildRetrofitInstance(apiUrl: String): T = Retrofit.Builder()
        .baseUrl(apiUrl)
        .client(httpClient)
        .addConverterFactory(kotlinxConverterFactory)
        .build()
        .create<T>()
}
