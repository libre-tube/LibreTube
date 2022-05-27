package com.github.libretube.util

import android.content.Context
import org.chromium.net.CronetEngine

class CronetHelper {
    companion object {
        private lateinit var engine: CronetEngine

        fun initCronet(context: Context) {
            this.engine = CronetEngine.Builder(context)
                .enableBrotli(true)
                .build()
        }

        fun getCronetEngine(): CronetEngine {
            return engine
        }
    }
}
