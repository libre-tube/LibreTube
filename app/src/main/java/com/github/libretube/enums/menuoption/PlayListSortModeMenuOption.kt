package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R

enum class PlayListSortModeMenuOption(
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int?,
): MenuOption {
    LEAST_RECENT(R.string.least_recent, R.drawable.ic_sort_by_oldest),
    MOST_RECENT(R.string.most_recent, R.drawable.ic_sort_by_newest),
    DURATION(R.string.duration, R.drawable.ic_sort_by_duration),
    DURATION_REVERSED(R.string.duration_reversed, R.drawable.ic_sort_by_duration_reversed),
    ALPHABETIC(R.string.alphabetic, R.drawable.ic_sort_by_alpha),
    ALPHABETIC_REVERSED(R.string.alphabetic_reversed, R.drawable.ic_sort_by_alpha_reversed),
}