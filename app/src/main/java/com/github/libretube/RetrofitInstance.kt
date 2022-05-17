package com.github.libretube

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import com.github.libretube.cronet.CronetCallFactory
import org.chromium.net.CronetEngine

object RetrofitInstance {
    lateinit var url: String
    lateinit var engine: CronetEngine
    val lazyMgr = resettableManager()
    val api: PipedApi by resettableLazy(lazyMgr) {

        Retrofit.Builder()
            .callFactory(CronetCallFactory.newBuilder(engine).build())
            .baseUrl(url)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }

}