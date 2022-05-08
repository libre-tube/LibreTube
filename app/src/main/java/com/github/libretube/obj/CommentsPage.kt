package com.github.libretube.obj

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentsPage(
        val comments: List<Comment> = listOf(),
        val disabled: Boolean? = null,
        val nextpage: String? = "",
){
        constructor(): this(emptyList(),null,"")
}
