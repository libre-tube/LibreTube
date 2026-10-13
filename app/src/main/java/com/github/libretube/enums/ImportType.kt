package com.github.libretube.enums

import androidx.annotation.StringRes
import com.github.libretube.R

enum class ImportType(@StringRes val stringRes: Int) {
    IMPORT_WATCH_HISTORY(R.string.watch_history),
    IMPORT_SUBSCRIPTIONS(R.string.subscriptions),
    IMPORT_PLAYLISTS(R.string.playlists)
}