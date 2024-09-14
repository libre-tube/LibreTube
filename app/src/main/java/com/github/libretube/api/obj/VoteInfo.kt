package com.github.libretube.api.obj

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class VoteInfo(
    val likes: Long,
    val rawDislikes: Long,
    val rawLikes: Long,
    val dislikes: Long,
    val rating: Float,
    val viewCount: Long,
    val deleted: Boolean
) : Parcelable
