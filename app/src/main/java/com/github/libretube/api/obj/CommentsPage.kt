package com.github.libretube.api.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentsPage(
    val comments: MutableList<com.github.libretube.api.obj.Comment> = arrayListOf(),
    val disabled: Boolean? = null,
    val nextpage: String? = ""
)
