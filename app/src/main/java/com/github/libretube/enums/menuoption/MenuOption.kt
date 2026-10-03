package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.obj.BottomSheetItem

interface MenuOption {
    @get:StringRes
    val titleStringRes: Int
    val drawableRes: Int?

    fun toBottomSheetItem(
        getString: (Int) -> String,
        preselectedItem: MenuOption? = null,
    ) = BottomSheetItem(
        title = getString(titleStringRes),
        drawable = drawableRes,
        isSelected =  this == preselectedItem,
    )
}