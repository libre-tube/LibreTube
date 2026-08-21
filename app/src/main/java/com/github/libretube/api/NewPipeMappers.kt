package com.github.libretube.api

import android.util.Base64
import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.Comment
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_CHANNEL
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_PLAYLIST
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_STREAM
import com.github.libretube.extensions.toID
import com.github.libretube.constants.ApiConstants
import kotlinx.serialization.Serializable
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream

internal fun VideoStream.toPipedStream() = PipedStream(
    url = content,
    codec = codec,
    format = format?.toString(),
    height = height,
    width = width,
    quality = getResolution(),
    mimeType = format?.mimeType,
    bitrate = bitrate,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    fps = fps,
    durationMs = itagItem?.approxDurationMs,
    contentLength = itagItem?.contentLength ?: 0L,
    itag = itagItem?.id,
    lastModified = itagItem?.lastModified,
    xtags = itagItem?.xtags,
)

internal fun AudioStream.toPipedStream() = PipedStream(
    url = content,
    format = format?.toString(),
    quality = "$averageBitrate bits",
    bitrate = bitrate,
    mimeType = format?.mimeType,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    durationMs = itagItem?.approxDurationMs,
    contentLength = itagItem?.contentLength ?: 0L,
    codec = codec,
    audioTrackId = audioTrackId,
    audioTrackName = audioTrackName,
    audioTrackLocale = audioLocale?.toLanguageTag(),
    audioTrackType = audioTrackType?.name,
    videoOnly = false,
    itag = itagItem?.id,
    lastModified = itagItem?.lastModified,
    isDrc = itagItem?.isDrc,
    xtags = itagItem?.xtags,
)

internal fun StreamInfoItem.toStreamItem(
    uploaderAvatarUrl: String? = null,
    feedInfo: StreamInfoItem? = null,
): StreamItem {
    val uploadDate = uploadDate ?: feedInfo?.uploadDate
    val textualUploadDate = textualUploadDate ?: feedInfo?.textualUploadDate

    return StreamItem(
        type = TYPE_STREAM,
        url = url.toID(),
        title = feedInfo?.name ?: name,
        uploaded = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: -1,
        uploadedDate = textualUploadDate ?: uploadDate?.offsetDateTime()?.toLocalDateTime()
            ?.toLocalDate()?.toString(),
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl?.toID(),
        uploaderAvatar = uploaderAvatarUrl ?: uploaderAvatars.maxByOrNull { it.height }?.url,
        thumbnail = thumbnails.maxByOrNull { it.height }?.url,
        duration = duration,
        views = viewCount,
        uploaderVerified = isUploaderVerified,
        shortDescription = shortDescription,
        isShort = isShortFormContent
    )
}

internal fun InfoItem.toContentItem(): ContentItem? = when (this) {
    is StreamInfoItem -> if (contentAvailability in arrayOf(
            ContentAvailability.AVAILABLE,
            ContentAvailability.UPCOMING,
            ContentAvailability.UNKNOWN
        )
    ) ContentItem(
        url = url.toID(),
        type = TYPE_STREAM,
        thumbnail = thumbnails.maxByOrNull { it.height }?.url.orEmpty(),
        title = name,
        uploaderAvatar = uploaderAvatars.maxByOrNull { it.height }?.url.orEmpty(),
        uploaderUrl = uploaderUrl.toID(),
        uploaderName = uploaderName,
        uploaded = uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: -1,
        isShort = isShortFormContent,
        views = viewCount,
        shortDescription = shortDescription,
        verified = isUploaderVerified,
        duration = duration
    ) else null

    is ChannelInfoItem -> ContentItem(
        url = url.toID(),
        name = name,
        type = TYPE_CHANNEL,
        thumbnail = thumbnails.maxByOrNull { it.height }?.url.orEmpty(),
        subscribers = subscriberCount,
        videos = streamCount
    )

    is PlaylistInfoItem -> ContentItem(
        url = url.toID(),
        type = TYPE_PLAYLIST,
        title = name,
        name = name,
        shortDescription = description.content,
        thumbnail = thumbnails.maxByOrNull { it.height }?.url.orEmpty(),
        videos = streamCount,
        uploaderVerified = isUploaderVerified,
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl?.toID()
    )

    else -> null
}

internal fun ChannelInfo.toChannel() = Channel(
    id = id,
    name = name,
    description = description,
    verified = isVerified,
    avatarUrl = avatars.maxByOrNull { it.height }?.url,
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    tabs = tabs.filterNot { it.contentFilters.contains(ChannelTabs.VIDEOS) }
        .map { ChannelTab(it.contentFilters.first().lowercase(), it.toTabDataString()) },
    subscriberCount = subscriberCount
)

internal fun PlaylistInfo.toPlaylist() = Playlist(
    name = name,
    description = description?.content,
    thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url,
    uploaderUrl = uploaderUrl.toID(),
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    uploader = uploaderName,
    uploaderAvatar = uploaderAvatars.maxByOrNull { it.height }?.url,
    videos = streamCount.toInt(),
    relatedStreams = relatedItems.map { (it as StreamInfoItem).toStreamItem() },
    nextpage = nextPage?.toNextPageString()
)

internal fun CommentsInfoItem.toComment() = Comment(
    author = uploaderName,
    commentId = commentId,
    commentText = commentText.content,
    commentedTime = textualUploadDate,
    commentedTimeMillis = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000),
    commentorUrl = uploaderUrl.toID(),
    hearted = isHeartedByUploader,
    creatorReplied = hasCreatorReply(),
    likeCount = likeCount.toLong(),
    pinned = isPinned,
    verified = isUploaderVerified,
    replyCount = replyCount.toLong(),
    repliesPage = replies?.toNextPageString(),
    thumbnail = thumbnails.maxByOrNull { it.height }?.url.orEmpty(),
    channelOwner = isChannelOwner
)

@Serializable
private data class NextPage(
    val url: String? = null,
    val id: String? = null,
    val ids: List<String>? = null,
    val cookies: Map<String, String>? = null,
    val body: String? = null
)

internal fun Page.toNextPageString() = JsonHelper.json.encodeToString(
    NextPage(url, id, ids, cookies, body?.let { Base64.encodeToString(it, Base64.DEFAULT) })
)

internal fun String.toPage(): Page = with(JsonHelper.json.decodeFromString<NextPage>(this)) {
    return Page(url, id, ids, cookies, body?.let { Base64.decode(it, Base64.DEFAULT) })
}

@Serializable
private data class TabData(
    val originalUrl: String? = null,
    val url: String? = null,
    val id: String? = null,
    val contentFilters: List<String>? = null,
    val sortFilter: String? = null,
)

internal fun ListLinkHandler.toTabDataString() = JsonHelper.json.encodeToString(
    TabData(originalUrl, url, id, contentFilters, sortFilter)
)

internal fun String.toListLinkHandler() = with(JsonHelper.json.decodeFromString<TabData>(this)) {
    ListLinkHandler(originalUrl, url, id, contentFilters, sortFilter)
}
