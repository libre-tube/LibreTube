package com.github.libretube.util

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    lateinit var url: String
    lateinit var authUrl: String
    val lazyMgr = resettableManager()
    val api: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
    val authApi: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .callFactory(CronetHelper.callFactory)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
}
