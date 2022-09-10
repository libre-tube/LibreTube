package com.github.libretube.api

import android.content.Context
import com.google.net.cronet.okhttptransport.CronetCallFactory
import org.chromium.net.CronetEngine

class CronetHelper {
    companion object {
        private lateinit var engine: CronetEngine
        lateinit var callFactory: CronetCallFactory

        fun initCronet(context: Context) {
            engine = CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L) // 1MiB
                .build()

            callFactory = CronetCallFactory.newBuilder(engine)
                .build()
        }

        fun getCronetEngine(): CronetEngine {
            return engine
        }
    }
}
