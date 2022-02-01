package com.github.libretube.obj

import com.github.libretube.obj.StreamItem

data class SearchResult(
    val items: List<SearchItem>? = listOf(),
    val nextpage: String? ="",
    val suggestion: String?="",
    val corrected: Boolean? = null
)
