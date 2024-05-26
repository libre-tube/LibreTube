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
        private val enabledFiltersSet: MutableSet<String> get() {
            val entryNames = try {
                PreferenceHelper
                    .getStringSet(SELECTED_FEED_FILTERS, entries.mapTo(mutableSetOf()) { it.name })
            } catch (e: ClassCastException) {
                // TODO: Remove the conversion code below.
                // Assume the old preference is present and convert it.
                val string = PreferenceHelper.getString(SELECTED_FEED_FILTERS, "")
                PreferenceHelper.remove(SELECTED_FEED_FILTERS)
                val set = string.split(',')
                    .mapTo(mutableSetOf()) { entries[it.toInt()].name }
                PreferenceHelper.putStringSet(SELECTED_FEED_FILTERS, set)
                set
            }

            return entryNames.toMutableSet()
        }
    }
}
