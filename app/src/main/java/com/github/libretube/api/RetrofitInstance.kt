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

    val authUrl: String
        get() = if (PreferenceHelper.getBoolean(PreferenceKeys.AUTH_INSTANCE_TOGGLE, false)) {
            PreferenceHelper.getString(PreferenceKeys.AUTH_INSTANCE, PIPED_API_URL)
        } else {
            PipedMediaServiceRepository.apiUrl
        }

    val apiLazyMgr = resettableManager()

    val kotlinxConverterFactory = JsonHelper.json
        .asConverterFactory("application/json".toMediaType())

    val httpClient by lazy { buildHttpClient() }

    val authApi by resettableLazy(apiLazyMgr) {
        buildRetrofitInstance<PipedAuthApi>(authUrl)
    }

    val externalApi = buildRetrofitInstance<ExternalApi>(PIPED_API_URL)

    inline fun <reified T : Any> buildRetrofitInstance(apiUrl: String): T = Retrofit.Builder()
        .baseUrl(apiUrl)
        .client(httpClient)
        .addConverterFactory(kotlinxConverterFactory)
        .build()
        .create<T>()

    private fun buildHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(RetryInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }
        .build()
}
