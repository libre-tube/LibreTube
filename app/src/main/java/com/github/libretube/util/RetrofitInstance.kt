package com.github.libretube.util

import com.google.net.cronet.okhttptransport.CronetCallFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    lateinit var url: String
    lateinit var authUrl: String
    val lazyMgr = resettableManager()
    private val callFactory: CronetCallFactory =
        CronetCallFactory.newBuilder(CronetHelper.getCronetEngine())
            .build()
    val api: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .callFactory(callFactory)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
    val authApi: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .callFactory(callFactory)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
}
