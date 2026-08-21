package com.github.libretube.helpers

import androidx.media3.common.Player
import com.github.libretube.LibreTubeApp
import com.github.libretube.R
import com.github.libretube.api.obj.Segment
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.SbSkipOptions

object SponsorBlockHelper {

    const val SPONSOR_HIGHLIGHT_CATEGORY = "poi_highlight"

    private val sbDefaultValues = mapOf(
        "sponsor" to SbSkipOptions.AUTOMATIC,
        "selfpromo" to SbSkipOptions.AUTOMATIC,
        "exclusive_access" to SbSkipOptions.AUTOMATIC,
    )

    val sponsorBlockEnabled: Boolean
        get() = PreferenceHelper.getBoolean("sb_enabled_key", true)

    val sponsorBlockNotifications: Boolean
        get() = PreferenceHelper.getBoolean("sb_notifications_key", true)

    private val sponsorBlockHighlights: Boolean
        get() = PreferenceHelper.getBoolean(PreferenceKeys.SB_HIGHLIGHTS, true)

    fun getCategories(): MutableMap<String, SbSkipOptions> {
        val categories: MutableMap<String, SbSkipOptions> = mutableMapOf()

        for (category in LibreTubeApp.instance.resources.getStringArray(R.array.sponsorBlockSegments)) {
            val defaultCategoryValue = sbDefaultValues.getOrDefault(category, SbSkipOptions.OFF)
            val skipOption = PreferenceHelper
                .getString("${category}_category", defaultCategoryValue.name)
                .let { SbSkipOptions.valueOf(it.uppercase()) }

            if (skipOption != SbSkipOptions.OFF) {
                categories[category] = skipOption
            }
        }

        if (sponsorBlockHighlights) categories[SPONSOR_HIGHLIGHT_CATEGORY] = SbSkipOptions.OFF
        return categories
    }

    fun Player.getCurrentSegment(
        segments: List<Segment>,
        sponsorBlockConfig: MutableMap<String, SbSkipOptions>,
    ): Pair<Segment, SbSkipOptions>? {
        for (segment in segments.filter { it.category != SPONSOR_HIGHLIGHT_CATEGORY }) {
            val (start, end) = segment.segmentStartAndEnd
            val (segmentStart, segmentEnd) = (start * 1000f).toLong() to (end * 1000f).toLong()

            if (segmentEnd - currentPosition in 0..1000) continue
            if (currentPosition !in segmentStart until segmentEnd) continue

            val key = sponsorBlockConfig[segment.category]
            if (key == SbSkipOptions.AUTOMATIC_ONCE && segment.skipped) continue

            return segment to (key ?: SbSkipOptions.AUTOMATIC)
        }

        return null
    }
}
