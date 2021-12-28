package xyz.btcland.libretube.obj

import xyz.btcland.libretube.obj.StreamItem

data class SearchResult(
    val items: List<SearchItem>? = listOf(),
    val nextpage: String? ="",
    val suggestion: String?="",
    val corrected: Boolean? = null
)
