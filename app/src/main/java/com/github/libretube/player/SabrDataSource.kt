package com.github.libretube.player

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.github.libretube.player.parser.CompositeBuffer
import com.github.libretube.player.parser.PlaybackRequest
import com.github.libretube.player.parser.SABRException
import com.github.libretube.player.parser.SabrClient
import java.io.IOException

@UnstableApi
class SabrDataSource(
    private val sabrClient: SabrClient,
) : BaseDataSource(true) {
    private var data: CompositeBuffer? = null

    class Factory(
        private val sabrClient: SabrClient
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = SabrDataSource(sabrClient)
    }

    override fun open(dataSpec: DataSpec): Long {
        val playbackRequest = dataSpec.customData as? PlaybackRequest

        transferInitializing(dataSpec)
        transferStarted(dataSpec)
        if (playbackRequest == null) {
            throw IOException("SabrDataSource: missing PlaybackRequest in DataSpec.customData")
        }
        val segment = try {
            sabrClient.getNextSegment(playbackRequest)
                ?: throw IOException("SabrDataSource: getNextSegment returned null")
        } catch (e: SABRException) {
            Log.e(TAG, "SABR streaming error: ${e.userFacingMessage}", e)
            throw IOException(e.userFacingMessage, e)
        } catch (e: Exception) {
            val errorMsg = buildString {
                append("Failed to get segment")
                if (playbackRequest != null) {
                    append(" #${playbackRequest.segment}")
                    append(" for itag=${playbackRequest.format.itag}")
                    append(" at position=${playbackRequest.playerPosition}ms")
                }
                append(": ${e.message ?: e.javaClass.simpleName}")
            }
            Log.e(TAG, errorMsg, e)
            throw IOException(errorMsg, e)
        }

        data = CompositeBuffer(segment.data)
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
        val currentData = data
        if (currentData == null || length == 0) {
            return 0;
        }

        if (!currentData.hasRemaining()) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = minOf(length, currentData.remaining())
        currentData.read(buffer, offset, bytesToRead)

        // this is not the actual amount of bytes transferred, since the SABR stream has some overhead,
        // e.g. for format metadata
        bytesTransferred(bytesToRead)
        return bytesToRead
    }

    companion object {
        private const val TAG = "SabrDataSource"
    }
}