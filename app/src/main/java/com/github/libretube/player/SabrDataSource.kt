package com.github.libretube.player

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.github.libretube.player.parser.PlaybackRequest
import com.github.libretube.player.parser.SabrClient
import java.io.IOException
import java.nio.ByteBuffer

@UnstableApi
class SabrDataSource(
    private val sabrClient: SabrClient,
) : BaseDataSource(true) {
    private var data: ByteBuffer? = null

    class Factory(
        private val sabrClient: SabrClient
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = SabrDataSource(sabrClient)
    }

    override fun open(dataSpec: DataSpec): Long {
        val playbackRequest = dataSpec.customData as PlaybackRequest?

        transferInitializing(dataSpec)
        transferStarted(dataSpec)
        val segment = runCatching { sabrClient.getNextSegment(playbackRequest!!) }
            .getOrNull() ?: throw IOException()
        data = ByteBuffer.wrap(segment.data())
        return data!!.remaining().toLong()
    }

    override fun getUri(): Uri? {
        if (data?.hasRemaining() != true) {
            // signal that this data source failed to be opened
            return null
        }
        return sabrClient.url.toUri()
    }

    override fun close() {
        transferEnded()
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

        // this is not the actual amount of bytes transferred, since the SABR stream has some overhead,
        // e.g. for format metadata
        bytesTransferred(bytesToRead)
        return bytesToRead
    }
}