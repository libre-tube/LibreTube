package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val items: List<ContentItem> = emptyList(),
    val nextpage: String? = null,
    val suggestion: String? = null,
    val corrected: Boolean? = null
)
