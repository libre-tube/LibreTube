package com.github.libretube.enums

import com.github.libretube.constants.PreferenceKeys.SELECTED_FEED_FILTERS
import com.github.libretube.helpers.PreferenceHelper

private val enabledFiltersSet get() = PreferenceHelper
    .getString(SELECTED_FEED_FILTERS, "1,2,3")
    .let { filtersPref -> if (filtersPref == "0") "1,2,3" else filtersPref } // For transition from 0 to 1,2,3 - legacy compatibility
    .split(',')
    .toMutableSet()

enum class ContentFilter(private val id: Int) {
    VIDEOS(1),
    SHORTS(2),
    LIVESTREAMS(3);

    fun isEnabled() = enabledFiltersSet.contains(id.toString())

    fun setState(enabled: Boolean) {
        val newFilters = enabledFiltersSet
            .apply { if (enabled) add(id.toString()) else remove(id.toString()) }
            .joinToString(",")

        PreferenceHelper.putString(SELECTED_FEED_FILTERS, newFilters)
    }

}