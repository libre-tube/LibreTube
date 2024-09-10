package com.github.libretube.obj

data class BottomSheetItem(
    var title: String,
    val drawable: Int? = null,
    val getCurrent: () -> String? = { null },
    val onClick: () -> Unit = {}
)
