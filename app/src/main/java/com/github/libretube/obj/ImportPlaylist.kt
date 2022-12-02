package com.github.libretube.obj

data class ImportPlaylist(
    val name: String? = null,
    val type: String? = null,
    val visibility: String? = null,
    val videos: List<String> = listOf()
)
