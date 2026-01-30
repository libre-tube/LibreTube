package com.github.libretube.api

import com.github.libretube.BuildConfig
import com.github.libretube.api.obj.DeArrowBody
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.PipedConfig
import com.github.libretube.api.obj.PipedInstance
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.SubmitSegmentResponse
import com.github.libretube.api.obj.VoteInfo
import com.github.libretube.obj.update.UpdateInfo
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

private const val GITHUB_API_URL = "https://api.github.com/repos/libre-tube/LibreTube/releases/latest"
private const val SB_API_URL = "https://sponsor.ajay.app"
private const val RYD_API_URL = "https://returnyoutubedislikeapi.com"

/**
 * Google API key for botguard requests.
 * TODO: Move to BuildConfig or secure configuration management.
 * WARNING: This key should be rotated regularly and not committed to version control.
 */
private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
private const val PIPED_INSTANCES_URL = "https://piped-instances.kavin.rocks"
private const val PIPED_INSTANCES_MARKDOWN_URL = "https://raw.githubusercontent.com/TeamPiped/documentation/refs/heads/main/content/docs/public-instances/index.md"

/**
 * External API interface for third-party services.
 * Handles communication with GitHub, SponsorBlock, ReturnYouTubeDislike, and other external APIs.
 */
interface ExternalApi {
    /**
     * Fetches the list of available Piped instances.
     * @param url The URL to fetch instances from (defaults to PIPED_INSTANCES_URL)
     * @return List of available Piped instances
     */
    @GET
    suspend fun getInstances(@Url url: String = PIPED_INSTANCES_URL): List<PipedInstance>

    /**
     * Fetches the instances list in markdown format.
     * @param url The URL to fetch markdown from (defaults to PIPED_INSTANCES_MARKDOWN_URL)
     * @return Response containing the markdown content
     */
    @GET
    suspend fun getInstancesMarkdown(@Url url: String = PIPED_INSTANCES_MARKDOWN_URL): Response<ResponseBody>

    /**
     * Retrieves configuration for a specific Piped instance.
     * @param url The instance URL
     * @return Configuration object for the instance
     */
    @GET("config")
    suspend fun getInstanceConfig(@Url url: String): PipedConfig

    /**
     * Fetches the latest LibreTube release information from GitHub.
     * @return Update information including version and download links
     */
    @GET(GITHUB_API_URL)
    suspend fun getLatestRelease(): UpdateInfo

    /**
     * Retrieves like/dislike vote counts from ReturnYouTubeDislike API.
     * @param videoId The YouTube video ID
     * @return Vote information including likes and dislikes
     */
    @GET("$RYD_API_URL/votes")
    suspend fun getVotes(@Query("videoId") videoId: String): VoteInfo

    /**
     * Submits a SponsorBlock segment for a video.
     * @param videoId The YouTube video ID
     * @param userID The SponsorBlock user ID
     * @param userAgent The user agent string
     * @param startTime Segment start time in seconds
     * @param endTime Segment end time in seconds
     * @param category The segment category (sponsor, intro, outro, etc.)
     * @param duration Optional video duration
     * @param description Optional segment description
     * @return List of submission responses
     */
    @POST("$SB_API_URL/api/skipSegments")
    suspend fun submitSegment(
        @Query("videoID") videoId: String,
        @Query("userID") userID: String,
        @Query("userAgent") userAgent: String,
        @Query("startTime") startTime: Float,
        @Query("endTime") endTime: Float,
        @Query("category") category: String,
        @Query("duration") duration: Float? = null,
        @Query("description") description: String = ""
    ): List<SubmitSegmentResponse>

    /**
     * Retrieves SponsorBlock segments for a video.
     * @param videoId The YouTube video ID
     * @param category List of segment categories to fetch
     * @param actionType Optional list of action types to filter
     * @return List of segment data
     */
    @GET("$SB_API_URL/api/skipSegments/{videoId}")
    suspend fun getSegments(
        @Path("videoId") videoId: String,
        @Query("category") category: List<String>,
        @Query("actionType") actionType: List<String>? = null
    ): List<SegmentData>

    /**
     * Submits DeArrow branding information (custom titles/thumbnails).
     * @param body The DeArrow submission data
     */
    @POST("$SB_API_URL/api/branding")
    suspend fun submitDeArrow(@Body body: DeArrowBody)

    /**
     * Votes on a SponsorBlock segment.
     * @param uuid The segment UUID
     * @param userID The SponsorBlock user ID
     * @param score Vote score: 0 for downvote, 1 for upvote, 20 for undo
     */
    @POST("$SB_API_URL/api/voteOnSponsorTime")
    suspend fun voteOnSponsorTime(
        @Query("UUID") uuid: String,
        @Query("userID") userID: String,
        @Query("type") score: Int
    )

    /**
     * Retrieves DeArrow content (custom titles/thumbnails) for a video.
     * @param videoId The YouTube video ID
     * @return Map of DeArrow content by type
     */
    @GET("$SB_API_URL/api/branding/{videoId}")
    suspend fun getDeArrowContent(@Path("videoId") videoId: String): Map<String, DeArrowContent>

    /**
     * Makes a botguard request to Google's API.
     * Used for proof-of-origin token generation.
     * @param url The botguard endpoint URL
     * @param jsonPayload The request payload
     * @return JSON response from the API
     */
    @Headers(
        "User-Agent: $USER_AGENT",
        "Accept: application/json",
        "Content-Type: application/json+protobuf",
        "x-goog-api-key: $GOOGLE_API_KEY",
        "x-user-agent: grpc-web-javascript/0.1",
    )
    @POST
    suspend fun botguardRequest(
        @Url url: String,
        @Body jsonPayload: List<String>
    ): JsonElement
}
