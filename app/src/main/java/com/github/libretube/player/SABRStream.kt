package com.github.libretube.player

import android.util.Log
import java.lang.RuntimeException
import java.nio.ByteBuffer

class SABRStream {
    private val TAG = SABRStream::class.simpleName

    /*
     * Pointer to the stream on the native side.
     */
    private var streamPtr = 0L
    private var audioChunks: MutableList<Segment> = mutableListOf()
    private var videoChunks: MutableList<Segment> = mutableListOf()

    init {
        System.loadLibrary("sabr")
        init()
    }

    private external fun init();

    /*
     * Create a new SABR stream.
     */
    private external fun create(
        video_id: String,
        url: String,
        ustreamer_config: ByteArray,
        po_token: ByteArray,
        audio_format_itag: Int,
        audio_format_last_modified: Long,
        video_format_itag: Int,
        video_format_last_modified: Long,
    ): Long;

    private external fun media(streamPtr: Long): Pair<LongArray, LongArray>?;


    private external fun destroy(streamPtr: Long);


    fun prepare(
        video_id: String,
        url: String,
        ustreamer_config: ByteArray,
        po_token: ByteArray,
        audio_format_itag: Int,
        audio_format_last_modified: Long,
        video_format_itag: Int,
        video_format_last_modified: Long,
    ) {
        streamPtr = create(
            video_id, url, ustreamer_config, po_token, audio_format_itag, audio_format_last_modified, video_format_itag, video_format_last_modified
        )
        Log.d(TAG, "start: created native stream at $streamPtr")
    }

    fun audio(): List<ByteBuffer> {
        if (audioChunks.isEmpty()) {
            assert(streamPtr != 0L) { "streamPtr is invalid" };
            updateChunks()
        }

        val segment = audioChunks.removeAt(0);
        return segment.data().toList()
    }

    fun video(): List<ByteBuffer> {
        if (videoChunks.isEmpty()) {
            assert(streamPtr != 0L) { "streamPtr is invalid" };
            updateChunks()
        }

        val segment = videoChunks.removeAt(0);
        return segment.data().toList()
    }

    private fun updateChunks(){
        assert(streamPtr != 0L) { "streamPtr is invalid" };

        try {
            val (audio, video) = media(streamPtr)?: return
            for (segPtr in audio) {
                audioChunks.add(Segment(segPtr))
            }

            for (segPtr in video) {
                videoChunks.add(Segment(segPtr))
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "updateChunks: ${e.message}")
            throw e
        }
    }

    fun destroy() {
        destroy(streamPtr)
        streamPtr = 0
    }
}

class Segment(private val nativePtr: Long) {
    private external fun duration(ptr: Long): Long
    private external fun len(ptr: Long): Int
    private external fun data(ptr: Long): Array<ByteBuffer>
    private external fun destroy(ptr: Long)

    fun duration(): Long = duration(nativePtr)
    fun len(): Int = len(nativePtr)
    fun data(): Array<ByteBuffer> = data(nativePtr)

    fun release() {
        destroy(nativePtr)
    }

    companion object {
        init {
            System.loadLibrary("sabr")
        }
    }
}
