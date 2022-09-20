package com.github.libretube.obj

data class NavBarItem(
    val resourceId: Int,
    val titleResource: Int,
    var isEnabled: Boolean = true
)
