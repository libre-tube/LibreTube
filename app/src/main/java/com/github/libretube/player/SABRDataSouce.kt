package com.github.libretube.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

@UnstableApi
class SABRDataSouce(isNetwork: Boolean) : BaseDataSource(isNetwork) {
    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SABRDataSouce(true)
    }

    override fun open(dataSpec: DataSpec): Long {
        TODO("Not yet implemented")
    }

    override fun getUri(): Uri? {
        TODO("Not yet implemented")
    }

    override fun close() {}

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        return C.RESULT_NOTHING_READ
    }
}