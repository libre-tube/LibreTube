package com.github.libretube.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.BaseMediaSource
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.github.libretube.api.obj.Streams


@OptIn(UnstableApi::class)
class SABRMediaSource
(
    private val mediaItem: MediaItem,
    private val videoId: String,
    private val streams: Streams,
) : BaseMediaSource() {
    private val TAG = SABRMediaSource::class.simpleName

    init {
        MediaLibraryInfo.registerModule("media3.exoplayer.sabr");
    }

    class Factory : MediaSource.Factory {
        private var streams: Streams? = null
        private var videoId: String? = null

        override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
            return this
        }

        override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
            return this
        }

        override fun getSupportedTypes(): IntArray {
            return intArrayOf(C.CONTENT_TYPE_OTHER)
        }

        fun setStreams(streams: Streams): MediaSource.Factory {
            this.streams = streams
            return this
        }

        fun setVideoId(videoId: String): MediaSource.Factory {
            this.videoId = videoId
            return this
        }

        override fun createMediaSource(mediaItem: MediaItem): MediaSource {
            return SABRMediaSource(mediaItem, videoId!!, streams!!)
        }
    }

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        notifySourceInfoRefreshed()
    }

    override fun releaseSourceInternal() {}

    override fun getMediaItem(): MediaItem = mediaItem

    override fun maybeThrowSourceInfoRefreshError() {}

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long
    ): MediaPeriod {
        return SABRMediaPeriod(
            uri = mediaItem.localConfiguration!!.uri,
            allocator,
            videoId,
            streams,
        )
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        (mediaPeriod as SABRMediaPeriod).release()
    }

    private fun notifySourceInfoRefreshed() {
        val timeline = SinglePeriodTimeline(
            C.TIME_UNSET,
            false,
            false,
            true,
            null,
            mediaItem
        );

        val placeholder = object : ForwardingTimeline(timeline) {
            override fun getWindow(
                windowIndex: Int, window: Window, defaultPositionProjectionUs: Long
            ): Window {
                super.getWindow(windowIndex, window, defaultPositionProjectionUs)
                window.isPlaceholder = true
                return window
            }

            override fun getPeriod(
                periodIndex: Int,
                period: Period,
                setIds: Boolean
            ): Period {
                super.getPeriod(periodIndex, period, setIds)
                period.isPlaceholder = true
                return period
            }
        }

        refreshSourceInfo(placeholder)

    }
}

