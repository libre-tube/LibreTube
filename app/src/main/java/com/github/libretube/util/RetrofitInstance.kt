package com.github.libretube.util

import com.github.libretube.resettableLazy
import com.github.libretube.resettableManager
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    lateinit var url: String
    val lazyMgr = resettableManager()
    val api: PipedApi by resettableLazy(lazyMgr) {
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
}
