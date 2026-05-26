package com.github.libretube.ui.extensions

import com.github.libretube.api.obj.Comment

fun List<Comment>.filterNonEmptyComments(): List<Comment> = filter { !it.commentText.isNullOrEmpty() }
