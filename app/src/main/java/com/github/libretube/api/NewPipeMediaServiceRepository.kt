package com.github.libretube.api

import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.MetaInfo
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.api.poToken.PoTokenGenerator
import com.github.libretube.constants.ApiConstants
import com.github.libretube.constants.YouTubeConstants
import com.github.libretube.extensions.sha256Sum
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.NewPipeExtractorInstance
import com.github.libretube.helpers.PlayerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import kotlin.time.toKotlinInstant

class NewPipeMediaServiceRepository : MediaServiceRepository {

    init {
        YoutubeStreamExtractor.setPoTokenProvider(PoTokenGenerator())
    }

    private val trendingCategories = TrendingCategory.entries.associate {
        when (it) {
            TrendingCategory.GAMING -> it to "trending_gaming"
            TrendingCategory.TRAILERS -> it to "trending_movies_and_shows"
            TrendingCategory.PODCASTS -> it to "trending_podcasts_episodes"
            TrendingCategory.MUSIC -> it to "trending_music"
            TrendingCategory.LIVE -> it to "live"
        }
    }

    override fun getTrendingCategories(): List<TrendingCategory> = trendingCategories.keys.toList()

    override suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem> =
        withContext(Dispatchers.IO) {
            val kioskList = NewPipeExtractorInstance.extractor.kioskList
            kioskList.forceContentCountry(ContentCountry(region))

            val extractor = kioskList.getExtractorById(trendingCategories[category], null)
            extractor.fetchPage()

            KioskInfo.getInfo(extractor).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toStreamItem() }
        }

    override suspend fun getStreams(videoId: String): Streams = withContext(Dispatchers.IO) {
        val respAsync = async {
            StreamInfo.getInfo("${YouTubeConstants.FRONTEND_URL}/watch?v=$videoId")
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
                .map { it.toStreamItem() },
            chapters = resp.streamSegments.map {
                ChapterSegment(
                    title = it.title,
                    image = it.previewUrl.orEmpty(),
                    start = it.startTimeSeconds.toLong()
                )
            },
            audioStreams = resp.audioStreams.map { it.toPipedStream() },
            videoStreams = resp.videoOnlyStreams.map { it.toPipedStream().copy(videoOnly = true) },
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
            },
            isShort = resp.isShortFormContent || (resp.videoStreams + resp.videoOnlyStreams)
                .firstOrNull()?.let { it.height > it.width } ?: false,
            serverAbrStreamingUrl = resp.serverAbrStreamingUrl,
            videoPlaybackUstreamerConfig = resp.ustreamerConfig,
        )
    }

    override suspend fun getSegments(
        videoId: String,
        category: List<String>,
        actionType: List<String>?
    ): SegmentData = RetrofitInstance.externalApi.getSegments(
        videoId.sha256Sum().substring(0, 4), category, actionType
    ).first { it.videoID == videoId }

    override suspend fun getDeArrowContent(videoId: String): DeArrowContent? =
        runCatching {
            RetrofitInstance.externalApi.getDeArrowContent(
                videoId.sha256Sum().substring(0, 4)
            )
        }.getOrDefault(emptyMap())[videoId]?.let { value ->
            value.copy(
                thumbnails = value.thumbnails.map { thumbnail ->
                    thumbnail.takeIf { it.original } ?: thumbnail.copy(
                        thumbnail = "${ApiConstants.DEARROW_THUMBNAIL_URL}?videoID=$videoId&time=${thumbnail.timestamp}"
                    )
                }
            )
        }

    override suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult {
        val queryHandler = NewPipeExtractorInstance.extractor.searchQHFactory.fromQuery(
            searchQuery, listOf(filter), null
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
            searchQuery, listOf(filter), null
        )
        val searchInfo = SearchInfo.getMoreItems(
            NewPipeExtractorInstance.extractor, queryHandler, nextPage.toPage()
        )
        return SearchResult(
            items = searchInfo.items.mapNotNull { it.toContentItem() },
            nextpage = searchInfo.nextPage?.toNextPageString()
        )
    }

    override suspend fun getSuggestions(query: String): List<String> =
        NewPipeExtractorInstance.extractor.suggestionExtractor.suggestionList(query)

    override suspend fun getChannel(channelId: String): Channel = withContext(Dispatchers.IO) {
        val channelUrl = "${YouTubeConstants.FRONTEND_URL}/channel/$channelId"
        val channelInfo = ChannelInfo.getInfo(NewPipeExtractorInstance.extractor, channelUrl)
        val channel = channelInfo.toChannel()

        val relatedVideos = getLatestVideos(channelInfo)
        channel.relatedStreams = relatedVideos.first
        channel.nextpage = relatedVideos.second

        channel
    }

    override suspend fun getChannelTab(data: String, nextPage: String?): ChannelTabResponse =
        withContext(Dispatchers.IO) {
            val linkListHandler = data.toListLinkHandler()

            val (items, newNextPage) = if (nextPage == null) {
                val resp = ChannelTabInfo.getInfo(NewPipeExtractorInstance.extractor, linkListHandler)
                resp.relatedItems to resp.nextPage
            } else {
                val resp = ChannelTabInfo.getMoreItems(
                    NewPipeExtractorInstance.extractor, linkListHandler, nextPage.toPage()
                )
                resp.items to resp.nextPage
            }

            ChannelTabResponse(
                content = items.mapNotNull { it.toContentItem() },
                nextpage = newNextPage?.toNextPageString()
            )
        }

    override suspend fun getChannelByName(channelName: String): Channel = withContext(Dispatchers.IO) {
        val channelUrl = "${YouTubeConstants.FRONTEND_URL}/c/$channelName"
        val channelInfo = ChannelInfo.getInfo(NewPipeExtractorInstance.extractor, channelUrl)
        val channel = channelInfo.toChannel()

        val relatedVideos = getLatestVideos(channelInfo)
        channel.relatedStreams = relatedVideos.first
        channel.nextpage = relatedVideos.second

        channel
    }

    override suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel =
        withContext(Dispatchers.IO) {
            val url = "${YouTubeConstants.FRONTEND_URL}/channel/$channelId/videos"
            val listLinkHandler = ListLinkHandler(url, url, channelId, listOf("videos"), "")
            val tab = getChannelTab(listLinkHandler.toTabDataString(), nextPage)
            Channel(
                relatedStreams = tab.content.map { it.toStreamItem() },
                nextpage = tab.nextpage
            )
        }

    override suspend fun getPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        val playlistUrl = "${YouTubeConstants.FRONTEND_URL}/playlist?list=$playlistId"
        PlaylistInfo.getInfo(playlistUrl).toPlaylist()
    }

    override suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist =
        withContext(Dispatchers.IO) {
            val playlistUrl = "${YouTubeConstants.FRONTEND_URL}/playlist?list=$playlistId"
            val playlistInfo = PlaylistInfo.getMoreItems(
                NewPipeExtractorInstance.extractor, playlistUrl, nextPage.toPage()
            )
            Playlist(
                relatedStreams = playlistInfo.items.map { it.toStreamItem() },
                nextpage = playlistInfo.nextPage?.toNextPageString()
            )
        }

    override suspend fun getComments(videoId: String): CommentsPage = withContext(Dispatchers.IO) {
        val url = "${YouTubeConstants.FRONTEND_URL}/watch?v=$videoId"
        val commentsInfo = CommentsInfo.getInfo(url)
        CommentsPage(
            nextpage = commentsInfo.nextPage?.toNextPageString(),
            disabled = commentsInfo.isCommentsDisabled,
            commentCount = commentsInfo.commentsCount.toLong(),
            comments = commentsInfo.relatedItems.map { it.toComment() }
        )
    }

    override suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage =
        withContext(Dispatchers.IO) {
            val url = "${YouTubeConstants.FRONTEND_URL}/watch?v=$videoId"
            val commentsInfo = CommentsInfo.getMoreItems(
                NewPipeExtractorInstance.extractor, url, nextPage.toPage()
            )
            CommentsPage(
                nextpage = commentsInfo.nextPage?.toNextPageString(),
                comments = commentsInfo.items.map { it.toComment() }
            )
        }

    private suspend fun getLatestVideos(channelInfo: ChannelInfo): Pair<List<StreamItem>, String?> {
        val relatedTab = channelInfo.tabs.find { it.contentFilters.contains("videos") }
            ?: return emptyList<StreamItem>() to null

        val relatedStreamsResp = getChannelTab(relatedTab.toTabDataString())
        return relatedStreamsResp.content.map { it.toStreamItem() } to relatedStreamsResp.nextpage
    }
}
