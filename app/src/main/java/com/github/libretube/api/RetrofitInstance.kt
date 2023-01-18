package com.github.libretube.api

import com.github.libretube.constants.PIPED_API_URL
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.util.PreferenceHelper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create

object RetrofitInstance {
    lateinit var url: String
    lateinit var authUrl: String
    val lazyMgr = resettableManager()
    private val jacksonConverterFactory = JacksonConverterFactory.create()
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val kotlinxConverterFactory = json.asConverterFactory("application/json".toMediaType())

    val api by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val authApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val externalApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create<ExternalApi>()
    }

    /**
     * Set the api urls needed for the [RetrofitInstance]
     */
    fun initialize() {
        url =
            PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, PIPED_API_URL)
        // set auth instance
        authUrl =
            if (
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
                url
            }
    }
}
