package com.github.libretube.obj.bottomsheetitem

open class BottomSheetItem<T : BottomSheetItemTitle>(
    val title: T,
    val drawable: Int? = null,
    val getCurrent: () -> T? = { null },
    val isSelected: Boolean = false,
    val onClick: () -> Unit = {},
)