package com.github.libretube.helpers

import com.github.libretube.util.NewPipeDownloaderImpl
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService

object NewPipeExtractorInstance {
    val extractor: StreamingService by lazy {
        NewPipe.getService(ServiceList.YouTube.serviceId)
    }

    fun init() {
        NewPipe.init(NewPipeDownloaderImpl())
    }
}