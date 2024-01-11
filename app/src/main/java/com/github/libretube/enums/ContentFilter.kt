package com.github.libretube.enums

import com.github.libretube.constants.PreferenceKeys.SELECTED_FEED_FILTERS
import com.github.libretube.helpers.PreferenceHelper

enum class ContentFilter {
    VIDEOS,
    SHORTS,
    LIVESTREAMS;

    fun isEnabled() = enabledFiltersSet.contains(ordinal.toString())

    fun setState(enabled: Boolean) {
        val newFilters = enabledFiltersSet
            .apply {if (enabled) add(ordinal.toString()) else remove(ordinal.toString()) }
            .joinToString(",")

        PreferenceHelper.putString(SELECTED_FEED_FILTERS, newFilters)
    }

    companion object {

        private val enabledFiltersSet get() = PreferenceHelper
            .getString(
                key = SELECTED_FEED_FILTERS,
                defValue = entries.joinToString(",") { it.ordinal.toString() }
            )
            .split(',')
            .toMutableSet()

    }

}