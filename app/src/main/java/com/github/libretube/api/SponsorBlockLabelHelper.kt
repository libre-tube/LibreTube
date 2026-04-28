package com.github.libretube.api

import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.api.obj.VideoLabelData
import com.github.libretube.extensions.sha256Sum

object SponsorBlockLabelHelper {
    private val cache = LruCache<String, VideoLabelData>(256)

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
    ): VideoLabelData? {
        // if we have the response cached, return it
        cache.get(videoId)?.let { return it }

        return runCatching {
            RetrofitInstance.externalApi.getVideoLabels(
                // use hashed video id for privacy
                // https://wiki.sponsor.ajay.app/w/API_Docs/Draft#GET_/api/videoLabels/:sha256HashPrefix
                videoId.sha256Sum().substring(0, 5),
            ).firstOrNull { it.videoID == videoId }
                .also { cache.put(videoId, it) }
        }.getOrNull()
    }

    /**
     * Returns a suitable drawable to display the category.
     *
     * If there is no matching icon, `null` is returned.
     */
    @DrawableRes
    fun categoryIcon(category: String?): Int? = when (category) {
        "exclusive_access" -> R.drawable.ic_exclusive_content
        "selfpromo" -> R.drawable.ic_selfpromo_content
        "sponsor" -> R.drawable.ic_paid_content
        else -> null
    }

    /**
     * Returns a suitable label to display the category.
     *
     * If there is no matching label, `null` is returned.
     */
    @StringRes
    fun categoryLabel(category: String?): Int? = when (category) {
        "sponsor" -> R.string.category_sponsor
        "exclusive_access" -> R.string.category_exclusive_access
        "selfpromo" -> R.string.category_selfpromo
        else -> null
    }
}