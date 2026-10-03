package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.media3.ui.AspectRatioFrameLayout
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
sealed class ResizeBottomSheetItem(
    val aspectRatioKey: Int,
    @StringRes titleStringRes: Int,
    @DrawableRes drawableRes: Int,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {
    companion object {
        val entries = listOf(
            ResizeModeFit,
            ResizeModeZoom,
            ResizeModeFill,
        )
    }

    data object ResizeModeFit : ResizeBottomSheetItem(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        R.string.resize_mode_fit,
        R.drawable.ic_resize_mode_fit,
    )

    data object ResizeModeZoom : ResizeBottomSheetItem(
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        R.string.resize_mode_zoom,
        R.drawable.ic_resize_mode_zoom,
    )

    data object ResizeModeFill : ResizeBottomSheetItem(
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        R.string.resize_mode_fill,
        R.drawable.ic_resize_mode_fill,
    )
}