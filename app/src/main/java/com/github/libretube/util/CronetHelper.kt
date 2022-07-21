package com.github.libretube.util

import android.content.Context
import com.google.net.cronet.okhttptransport.CronetCallFactory
import org.chromium.net.CronetEngine

class CronetHelper {
    companion object {
        private lateinit var engine: CronetEngine
        lateinit var callFactory: CronetCallFactory

        fun initCronet(context: Context) {
            this.engine = CronetEngine.Builder(context)
                .enableBrotli(true)
                .build()
            callFactory = CronetCallFactory.newBuilder(this.engine)
                .build()
        }

        fun getCronetEngine(): CronetEngine {
            return engine
        }
    }
}
