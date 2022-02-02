package com.github.libretube

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    val api: PipedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://pipedapi.kavin.rocks/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
}