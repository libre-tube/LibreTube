package com.github.libretube.api

import android.util.Base64
import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Comment
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.MetaInfo
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_CHANNEL
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_PLAYLIST
import com.github.libretube.api.obj.StreamItem.Companion.TYPE_STREAM
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.api.poToken.PoTokenGenerator
import com.github.libretube.extensions.parallelMap
import com.github.libretube.extensions.sha256Sum
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.VideoStream
import kotlin.time.toKotlinInstant


private fun VideoStream.toPipedStream() = PipedStream(
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
    contentLength = itagItem?.contentLength ?: 0L
)

private fun AudioStream.toPipedStream() = PipedStream(
    url = content,
    format = format?.toString(),
    quality = "$averageBitrate bits",
    bitrate = bitrate,
    mimeType = format?.mimeType,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    contentLength = itagItem?.contentLength ?: 0L,
    codec = codec,
    audioTrackId = audioTrackId,
    audioTrackName = audioTrackName,
    audioTrackLocale = audioLocale?.toLanguageTag(),
    audioTrackType = audioTrackType?.name,
    videoOnly = false
)

fun StreamInfoItem.toStreamItem(
    uploaderAvatarUrl: String? = null,
    feedInfo: StreamInfoItem? = null,
): StreamItem {
    val uploadDate = uploadDate ?: feedInfo?.uploadDate
    val textualUploadDate = textualUploadDate ?: feedInfo?.textualUploadDate

    return StreamItem(
        type = TYPE_STREAM,
        url = url.toID(),
        // if available prefer the RSS feed title, since it's untranslated
        title = feedInfo?.name ?: name,
        uploaded = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: -1,
        uploadedDate = textualUploadDate ?: uploadDate?.offsetDateTime()?.toLocalDateTime()
            ?.toLocalDate()
            ?.toString(),
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

fun InfoItem.toContentItem() = when (this) {
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

fun ChannelInfo.toChannel() = Channel(
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

fun PlaylistInfo.toPlaylist() = Playlist(
    name = name,
    description = description?.content,
    thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url,
    uploaderUrl = uploaderUrl.toID(),
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    uploader = uploaderName,
    uploaderAvatar = uploaderAvatars.maxByOrNull { it.height }?.url,
    videos = streamCount.toInt(),
    relatedStreams = relatedItems.map { it.toStreamItem() },
    nextpage = nextPage?.toNextPageString()
)

fun CommentsInfoItem.toComment() = Comment(
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

// the following classes are necessary because kotlinx can't deserialize
// classes from external libraries as they're not annotated
@Serializable
private data class NextPage(
    val url: String? = null,
    val id: String? = null,
    val ids: List<String>? = null,
    val cookies: Map<String, String>? = null,
    val body: String? = null
)

fun Page.toNextPageString() = JsonHelper.json.encodeToString(
    NextPage(url, id, ids, cookies, body?.let { Base64.encodeToString(it, Base64.DEFAULT) })
)

fun String.toPage(): Page = with(JsonHelper.json.decodeFromString<NextPage>(this)) {
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

fun ListLinkHandler.toTabDataString() = JsonHelper.json.encodeToString(
    TabData(originalUrl, url, id, contentFilters, sortFilter)
)

fun String.toListLinkHandler() = with(JsonHelper.json.decodeFromString<TabData>(this)) {
    ListLinkHandler(originalUrl, url, id, contentFilters, sortFilter)
}

class NewPipeMediaServiceRepository : MediaServiceRepository {
    init {
        YoutubeStreamExtractor.setPoTokenProvider(PoTokenGenerator());
    }

    // see https://github.com/TeamNewPipe/NewPipeExtractor/tree/dev/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/extractors/kiosk
    private val trendingCategories = TrendingCategory.entries.associate {
        when (it) {
            TrendingCategory.GAMING -> it to "trending_gaming"
            TrendingCategory.TRAILERS -> it to "trending_movies_and_shows"
            TrendingCategory.PODCASTS -> it to "trending_podcasts_episodes"
            TrendingCategory.MUSIC -> it to "trending_music"
            TrendingCategory.LIVE -> it to "live"
        }
    }

    override fun getTrendingCategories(): List<TrendingCategory> =
        trendingCategories.keys.toList()

    override suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem> {
        val kioskList = NewPipeExtractorInstance.extractor.kioskList
        kioskList.forceContentCountry(ContentCountry(region))

        val kioskId = trendingCategories[category]
        val extractor = kioskList.getExtractorById(kioskId, null)
        extractor.fetchPage()

        val info = KioskInfo.getInfo(extractor)
        return info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toStreamItem() }
    }

    override suspend fun getStreams(videoId: String): Streams = withContext(Dispatchers.IO) {
        val respAsync = async {
            StreamInfo.getInfo("$YOUTUBE_FRONTEND_URL/watch?v=$videoId")
        }
        val dislikesAsync = async {
            if (PlayerHelper.localRYD) runCatching {
                RetrofitInstance.externalApi.getVotes(videoId).dislikes
            }.getOrElse { -1 } else -1
        }
        val (resp, dislikes) = Pair(respAsync.await(), dislikesAsync.await())

        Streams(
            title = resp.name,
            description = resp.description.content,
            uploader = resp.uploaderName,
            uploaderAvatar = resp.uploaderAvatars.maxBy { it.height }.url,
            uploaderUrl = resp.uploaderUrl.toID(),
            uploaderVerified = resp.isUploaderVerified,
            uploaderSubscriberCount = resp.uploaderSubscriberCount,
            category = resp.category,
            views = resp.viewCount,
            likes = resp.likeCount,
            dislikes = dislikes,
            license = resp.licence,
            hls = resp.hlsUrl,
            dash = resp.dashMpdUrl,
            tags = resp.tags,
            metaInfo = resp.metaInfo.map {
                MetaInfo(
                    it.title,
                    it.content.content,
                    it.urls.map { url -> url.toString() },
                    it.urlTexts
                )
            },
            visibility = resp.privacy.name.lowercase(),
            duration = resp.duration,
            uploadTimestamp = resp.uploadDate.offsetDateTime().toInstant().toKotlinInstant(),
            uploaded = resp.uploadDate.offsetDateTime().toEpochSecond() * 1000,
            thumbnailUrl = resp.thumbnails.maxBy { it.height }.url,
            relatedStreams = resp.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item -> item.toStreamItem() },
            chapters = resp.streamSegments.map {
                ChapterSegment(
                    title = it.title,
                    image = it.previewUrl.orEmpty(),
                    start = it.startTimeSeconds.toLong()
                )
            },
            audioStreams = resp.audioStreams.map { it.toPipedStream() },
            videoStreams = resp.videoOnlyStreams.map { it.toPipedStream().copy(videoOnly = true) } +
                    resp.videoStreams.map { it.toPipedStream().copy(videoOnly = false) },
            previewFrames = resp.previewFrames.map {
                PreviewFrames(
                    it.urls,
                    it.frameWidth,
                    it.frameHeight,
                    it.totalCount,
                    it.durationPerFrame.toLong(),
                    it.framesPerPageX,
                    it.framesPerPageY
                )
            },
            subtitles = resp.subtitles.map {
                Subtitle(
                    it.content,
                    it.format?.mimeType,
                    it.displayLanguageName,
                    it.languageTag,
                    it.isAutoGenerated
                )
            }
        )
    }

    override suspend fun getSegments(
        videoId: String, category: List<String>, actionType: List<String>?
    ): SegmentData = RetrofitInstance.externalApi.getSegments(
        // use hashed video id for privacy
        // https://wiki.sponsor.ajay.app/w/API_Docs#GET_/api/skipSegments/:sha256HashPrefix
        videoId.sha256Sum().substring(0, 4), category, actionType
    ).first { it.videoID == videoId }

    override suspend fun getDeArrowContent(videoId: String): DeArrowContent? =
        runCatching {
            RetrofitInstance.externalApi.getDeArrowContent(
                // use hashed video id for privacy
                // https://wiki.sponsor.ajay.app/w/API_Docs/DeArrow#GET_/api/branding/:sha256HashPrefix
                videoId.sha256Sum().substring(0, 4)
            )
        }.getOrDefault(emptyMap())[videoId]?.let { value ->
            value.copy(
                thumbnails = value.thumbnails.map { thumbnail ->
                    thumbnail.takeIf { it.original } ?: thumbnail.copy(
                        thumbnail = "${DEARROW_THUMBNAIL_URL}?videoID=$videoId&time=${thumbnail.timestamp}"
                    )
                })
        }

    override suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult {
        val queryHandler = NewPipeExtractorInstance.extractor.searchQHFactory.fromQuery(
            searchQuery,
            listOf(filter),
            null
        )
        val searchInfo = SearchInfo.getInfo(NewPipeExtractorInstance.extractor, queryHandler)

        return SearchResult(
            items = searchInfo.relatedItems.mapNotNull { it.toContentItem() },
            nextpage = searchInfo.nextPage?.toNextPageString(),
            suggestion = searchInfo.searchSuggestion,
            corrected = searchInfo.isCorrectedSearch
        )
    }

    override suspend fun getSearchResultsNextPage(
        searchQuery: String,
        filter: String,
        nextPage: String
    ): SearchResult {
        val queryHandler = NewPipeExtractorInstance.extractor.searchQHFactory.fromQuery(
            searchQuery,
            listOf(filter),
            null
        )
        val searchInfo = SearchInfo.getMoreItems(
            NewPipeExtractorInstance.extractor,
            queryHandler,
            nextPage.toPage()
        )
        return SearchResult(
            items = searchInfo.items.mapNotNull { it.toContentItem() },
            nextpage = searchInfo.nextPage?.toNextPageString()
        )
    }

    override suspend fun getSuggestions(query: String): List<String> {
        return NewPipeExtractorInstance.extractor.suggestionExtractor.suggestionList(query)
    }

    private suspend fun getLatestVideos(channelInfo: ChannelInfo): Pair<List<StreamItem>, String?> {
        val relatedTab = channelInfo.tabs.find { it.contentFilters.contains(ChannelTabs.VIDEOS) }
            ?: return emptyList<StreamItem>() to null

        val relatedStreamsResp = getChannelTab(relatedTab.toTabDataString())
        return relatedStreamsResp.content.map { it.toStreamItem() } to relatedStreamsResp.nextpage
    }

    override suspend fun getChannel(channelId: String): Channel {
        val channelUrl = "$YOUTUBE_FRONTEND_URL/channel/${channelId}"
        val channelInfo = ChannelInfo.getInfo(NewPipeExtractorInstance.extractor, channelUrl)

        val channel = channelInfo.toChannel()

        val relatedVideos = getLatestVideos(channelInfo)
        channel.relatedStreams = relatedVideos.first
        channel.nextpage = relatedVideos.second

        return channel
    }

    override suspend fun getChannelTab(data: String, nextPage: String?): ChannelTabResponse {
        val linkListHandler = data.toListLinkHandler()

        val (items, newNextPage) = if (nextPage == null) {
            val resp = ChannelTabInfo.getInfo(NewPipeExtractorInstance.extractor, linkListHandler)
            resp.relatedItems to resp.nextPage
        } else {
            val resp = ChannelTabInfo.getMoreItems(
                NewPipeExtractorInstance.extractor,
                linkListHandler,
                nextPage.toPage()
            )
            resp.items to resp.nextPage
        }

        return ChannelTabResponse(
            content = items.mapNotNull { it.toContentItem() },
            nextpage = newNextPage?.toNextPageString()
        )
    }

    override suspend fun getChannelByName(channelName: String): Channel {
        val channelUrl = "$YOUTUBE_FRONTEND_URL/c/${channelName}"
        val channelInfo = ChannelInfo.getInfo(NewPipeExtractorInstance.extractor, channelUrl)

        val channel = channelInfo.toChannel()

        val relatedVideos = getLatestVideos(channelInfo)
        channel.relatedStreams = relatedVideos.first
        channel.nextpage = relatedVideos.second

        return channel
    }

    override suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel {
        val url = "${YOUTUBE_FRONTEND_URL}/channel/${channelId}/videos"
        val listLinkHandler = ListLinkHandler(url, url, channelId, listOf("videos"), "")
        val tab = getChannelTab(listLinkHandler.toTabDataString(), nextPage)
        return Channel(
            relatedStreams = tab.content.map { it.toStreamItem() },
            nextpage = tab.nextpage
        )
    }

    override suspend fun getPlaylist(playlistId: String): Playlist {
        val playlistUrl = "${YOUTUBE_FRONTEND_URL}/playlist?list=${playlistId}"
        val playlistInfo = PlaylistInfo.getInfo(playlistUrl)

        return playlistInfo.toPlaylist()
    }

    override suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist {
        val playlistUrl = "${YOUTUBE_FRONTEND_URL}/playlist?list=${playlistId}"
        val playlistInfo = PlaylistInfo.getMoreItems(
            NewPipeExtractorInstance.extractor,
            playlistUrl,
            nextPage.toPage()
        )

        return Playlist(
            relatedStreams = playlistInfo.items.map { it.toStreamItem() },
            nextpage = playlistInfo.nextPage?.toNextPageString()
        )
    }

    override suspend fun getComments(videoId: String): CommentsPage {
        val url = "${YOUTUBE_FRONTEND_URL}/watch?v=$videoId"
        val commentsInfo = CommentsInfo.getInfo(url)

        return CommentsPage(
            nextpage = commentsInfo.nextPage?.toNextPageString(),
            disabled = commentsInfo.isCommentsDisabled,
            commentCount = commentsInfo.commentsCount.toLong(),
            comments = commentsInfo.relatedItems.map { it.toComment() }
        )
    }

    override suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage {
        val url = "${YOUTUBE_FRONTEND_URL}/watch?v=$videoId"
        val commentsInfo = CommentsInfo.getMoreItems(
            NewPipeExtractorInstance.extractor,
            url,
            nextPage.toPage()
        )

        return CommentsPage(
            nextpage = commentsInfo.nextPage?.toNextPageString(),
            comments = commentsInfo.items.map { it.toComment() }
        )
    }

    companion object {
        private const val DEARROW_THUMBNAIL_URL = "https://dearrow-thumb.ajay.app/api/v1/getThumbnail"
    }
}
