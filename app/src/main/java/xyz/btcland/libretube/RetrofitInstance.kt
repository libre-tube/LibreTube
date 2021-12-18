package xyz.btcland.libretube

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitInstance {
    val api: PipedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://piped-api.alefvanoon.xyz/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }
}