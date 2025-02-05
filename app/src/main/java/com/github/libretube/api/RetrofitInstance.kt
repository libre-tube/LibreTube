package com.github.libretube.api

import com.github.libretube.BuildConfig
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

object RetrofitInstance {
    private const val PIPED_API_URL = "https://pipedapi.kavin.rocks"
    val apiUrl get() = PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, PIPED_API_URL)
    val authUrl
        get() = when (
            PreferenceHelper.getBoolean(
                PreferenceKeys.AUTH_INSTANCE_TOGGLE,
                false
            )
        ) {
            true -> PreferenceHelper.getString(
                PreferenceKeys.AUTH_INSTANCE,
                PIPED_API_URL
            )

            false -> apiUrl
        }

    val lazyMgr = resettableManager()
    private val kotlinxConverterFactory = JsonHelper.json
        .asConverterFactory("application/json".toMediaType())

    private val httpClient by lazy { buildClient() }

    val api by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(httpClient)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val authApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .client(httpClient)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val externalApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(httpClient)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<ExternalApi>()
    }

    private fun buildClient(): OkHttpClient {
        val httpClient = OkHttpClient().newBuilder()

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            httpClient.addInterceptor(loggingInterceptor)
        }

        return httpClient.build()
    }
}
