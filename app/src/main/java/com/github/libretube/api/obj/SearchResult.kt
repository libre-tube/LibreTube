package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchResult(
    val items: MutableList<ContentItem>? = arrayListOf(),
    val nextpage: String? = "",
    val suggestion: String? = "",
    val corrected: Boolean? = null
)
