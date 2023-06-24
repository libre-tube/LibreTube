package com.github.libretube.obj.update

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val assets: List<Asset> = emptyList(),
    @SerialName("assets_url") val assetsUrl: String,
    val author: User,
    val body: String,
    @SerialName("created_at") val createdAt: Instant,
    val draft: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    val id: Int,
    @SerialName("mentions_count") val mentionsCount: Int,
    val name: String,
    @SerialName("node_id") val nodeId: String,
    val prerelease: Boolean,
    @SerialName("published_at") val publishedAt: Instant,
    val reactions: Reactions,
    @SerialName("tag_name") val tagName: String,
    @SerialName("tarball_url") val tarballUrl: String,
    @SerialName("target_commitish") val targetCommitish: String,
    @SerialName("upload_url") val uploadUrl: String,
    val url: String,
    @SerialName("zipball_url") val zipballUrl: String
)
