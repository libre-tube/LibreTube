package com.github.libretube.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.github.libretube.player.parser.SabrClient
import misc.Common
import java.nio.ByteBuffer

@UnstableApi
class SabrDataSource() : BaseDataSource(true) {
    private var data: ByteBuffer? = null

    class Factory() : DataSource.Factory {
        override fun createDataSource(): DataSource = SabrDataSource()
    }

    override fun open(dataSpec: DataSpec): Long {
        val format = dataSpec.customData as Common.FormatId?
        val segment = SabrClient.data(format!!.itag)
        val totalSize = segment.sumOf { it.length() }
        val buffer = ByteBuffer.allocate(totalSize)
        segment.forEach { buffer.put(it.data()) }
        // prepare for reading
        buffer.flip()
        data = buffer

        return totalSize.toLong()
    }

    override fun getUri(): Uri? = data?.let { SabrClient.url.toUri() }

    override fun close() {
        data = null
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        assert(data != null)
        if (length == 0) {
            return 0;
        }

        if (!data!!.hasRemaining()) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = minOf(length, data!!.remaining())
        data = data!!.get(buffer, offset, bytesToRead)
        return bytesToRead
    }
}