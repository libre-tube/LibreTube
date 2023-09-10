package com.github.libretube.api

import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.create

object RetrofitInstance {
    private const val PIPED_API_URL = "https://pipedapi.kavin.rocks"
    private val url get() = PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, PIPED_API_URL)
    private val authUrl
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
            false -> url
        }

    val lazyMgr = resettableManager()
    private val kotlinxConverterFactory = JsonHelper.json
        .asConverterFactory("application/json".toMediaType())

    val api by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val authApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<PipedApi>()
    }

    val externalApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(kotlinxConverterFactory)
            .build()
            .create<ExternalApi>()
    }
}
