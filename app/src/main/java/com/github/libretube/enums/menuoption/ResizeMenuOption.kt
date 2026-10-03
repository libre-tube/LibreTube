package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import androidx.media3.ui.AspectRatioFrameLayout
import com.github.libretube.R

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
enum class ResizeMenuOption(
    val aspectRatioKey: Int,
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int
) : MenuOption {
    RESIZE_MODE_FIT(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        R.string.resize_mode_fit,
        R.drawable.ic_resize_mode_fit,
    ),
    RESIZE_MODE_ZOOM(
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        R.string.resize_mode_zoom,
        R.drawable.ic_resize_mode_zoom,
    ),
    RESIZE_MODE_FILL(
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        R.string.resize_mode_fill,
        R.drawable.ic_resize_mode_fill,
    ),
}