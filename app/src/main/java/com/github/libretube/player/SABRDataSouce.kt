package com.github.libretube.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

@UnstableApi
class SABRDataSouce(isNetwork: Boolean, private val stream: SABRStream, private val itag: Int) : BaseDataSource(isNetwork) {
    class Factory : DataSource.Factory {
        private lateinit var stream: SABRStream
        private var itag = -1

        fun setStream(stream: SABRStream) {
            this.stream = stream
        }

        fun setItag(itag: Int) {
            this.itag = itag
        }

        override fun createDataSource(): DataSource = SABRDataSouce(true, stream, itag)
    }

    override fun open(dataSpec: DataSpec): Long {
        //TODO: get itag from uri or customData
        // dataSpec.customData
        // dataSpec.uri
        TODO("Not yet implemented")
    }

    override fun getUri(): Uri? = stream.sabrUri

    override fun close() {}

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        stream.video()
        return C.RESULT_NOTHING_READ
    }
}