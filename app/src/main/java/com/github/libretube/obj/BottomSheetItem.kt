package com.github.libretube.obj

data class BottomSheetItem(
    val title: String,
    val drawable: Int? = null,
    val currentValue: String? = null,
    val onClick: () -> Unit = {}
)
