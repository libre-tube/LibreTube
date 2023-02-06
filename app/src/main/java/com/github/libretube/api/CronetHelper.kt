package com.github.libretube.api

import com.github.libretube.LibreTubeApp
import com.google.net.cronet.okhttptransport.CronetCallFactory
import org.chromium.net.CronetEngine

object CronetHelper {
    val cronetEngine: CronetEngine = CronetEngine.Builder(LibreTubeApp.instance)
        .enableHttp2(true)
        .enableQuic(true)
        .enableBrotli(true)
        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 1024L * 1024L) // 1MiB
        .build()

    val callFactory: CronetCallFactory = CronetCallFactory.newBuilder(cronetEngine).build()
}
