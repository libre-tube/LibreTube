package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentsPage(
    val comments: MutableList<Comment> = arrayListOf(),
    val disabled: Boolean? = null,
    val nextpage: String? = ""
)
