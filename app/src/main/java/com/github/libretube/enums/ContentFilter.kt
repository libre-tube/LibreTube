package com.github.libretube.enums

import com.github.libretube.constants.PreferenceKeys.SELECTED_FEED_FILTERS
import com.github.libretube.helpers.PreferenceHelper

enum class ContentFilter {
    VIDEOS,
    SHORTS,
    LIVESTREAMS;

    var isEnabled
        get() = name in enabledFiltersSet
        set(enabled) {
            val newFilters = enabledFiltersSet
                .apply { if (enabled) add(name) else remove(name) }

            PreferenceHelper.putStringSet(SELECTED_FEED_FILTERS, newFilters)
        }

    companion object {
        private val enabledFiltersSet get() = PreferenceHelper
            .getStringSet(SELECTED_FEED_FILTERS, entries.mapTo(mutableSetOf()) { it.name })
            .toMutableSet()
    }
}
