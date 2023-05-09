package com.github.libretube.obj.update

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

@Serializable
data class Asset(
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("download_count") val downloadCount: Int,
    val id: Int,
    val label: JsonElement = JsonNull,
    val name: String,
    @SerialName("node_id") val nodeId: String,
    val size: Int,
    val state: String,
    @SerialName("updated_at") val updatedAt: Instant,
    val uploader: User,
    val url: String,
)
