package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResult(
    val items: MutableList<SearchItem>? = arrayListOf(),
    val nextpage: String? = "",
    val suggestion: String? = "",
    val corrected: Boolean? = null
)
