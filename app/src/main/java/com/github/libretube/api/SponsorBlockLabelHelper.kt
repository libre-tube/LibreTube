package com.github.libretube.api

import android.util.LruCache
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.extensions.sha256Sum

private data class CacheObject(val value: SegmentData?)

object SponsorBlockLabelHelper {
    // we cannot use segment data directly, as LruCache expects non-null objects, but we also want to cache unlabeled videos
    private val memoryCache = LruCache<String, CacheObject?>(256)

    /**
     * Returns the full video labels for a video.
     *
     * A full-video label is used, when the entire video is connected to the label,
     * such as a sponsored video, or exclusive-access from a company.
     *
     * See https://wiki.sponsor.ajay.app/w/Full_Video_Labels for more details.
     */
    suspend fun getVideoLabels(
        videoId: String,
    ): SegmentData? {
        // if we have the response cached, return it
        memoryCache.get(videoId)?.let { return it.value }

        return runCatching {
            RetrofitInstance.externalApi.getVideoLabels(
                // use hashed video id for privacy
                // https://wiki.sponsor.ajay.app/w/API_Docs/Draft#GET_/api/videoLabels/:sha256HashPrefix
                videoId.sha256Sum().substring(0, 5),
            ).firstOrNull { it.videoID == videoId }
        }.getOrNull().also { memoryCache.put(videoId, CacheObject(it)) }
    }
}