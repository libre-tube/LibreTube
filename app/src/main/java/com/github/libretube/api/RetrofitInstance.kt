package com.github.libretube.api

import com.github.libretube.constants.PIPED_API_URL
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.util.PreferenceHelper
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    lateinit var url: String
    lateinit var authUrl: String
    val lazyMgr = resettableManager()
    val jacksonConverterFactory = JacksonConverterFactory.create()

    val api: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create(PipedApi::class.java)
    }

    val authApi: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create(PipedApi::class.java)
    }

    val externalApi: ExternalApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create(ExternalApi::class.java)
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
