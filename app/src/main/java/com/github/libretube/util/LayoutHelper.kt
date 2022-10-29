package com.github.libretube.util

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys

object LayoutHelper {
    fun getVideoLayout(context: Context) = if (
        PreferenceHelper.getBoolean(
            PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
            false
        )
    ) {
        LinearLayoutManager(context)
    } else {
        GridLayoutManager(
            context,
            PreferenceHelper.getString(
                PreferenceKeys.GRID_COLUMNS,
                context.resources.getInteger(R.integer.grid_items).toString()
            ).toInt()
        )
    }
}
