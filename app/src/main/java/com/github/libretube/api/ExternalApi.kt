package com.github.libretube.api

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
private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
private const val PIPED_INSTANCES_URL = "https://piped-instances.kavin.rocks"
private const val PIPED_INSTANCES_MARKDOWN_URL = "https://raw.githubusercontent.com/TeamPiped/documentation/refs/heads/main/content/docs/public-instances/index.md"

interface ExternalApi {
    // only for fetching servers list
    @GET
    suspend fun getInstances(@Url url: String = PIPED_INSTANCES_URL): List<PipedInstance>

    @GET
    suspend fun getInstancesMarkdown(@Url url: String = PIPED_INSTANCES_MARKDOWN_URL): Response<ResponseBody>

    @GET("config")
    suspend fun getInstanceConfig(@Url url: String): PipedConfig

    // fetch latest version info
    @GET(GITHUB_API_URL)
    suspend fun getLatestRelease(): UpdateInfo

    @GET("$RYD_API_URL/votes")
    suspend fun getVotes(@Query("videoId") videoId: String): VoteInfo

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

    @GET("$SB_API_URL/api/skipSegments/{videoId}")
    suspend fun getSegments(
        @Path("videoId") videoId: String,
        @Query("category") category: List<String>,
        @Query("actionType") actionType: List<String>? = null
    ): List<SegmentData>

    @GET("$SB_API_URL/api/videoLabels/{videoId}")
    suspend fun getVideoLabels(
        @Path("videoId") videoId: String,
    ): List<SegmentData>

    @POST("$SB_API_URL/api/branding")
    suspend fun submitDeArrow(@Body body: DeArrowBody)

    /**
     * @param score: 0 for downvote, 1 for upvote, 20 for undoing previous vote (if existent)
     */
    @POST("$SB_API_URL/api/voteOnSponsorTime")
    suspend fun voteOnSponsorTime(
        @Query("UUID") uuid: String,
        @Query("userID") userID: String,
        @Query("type") score: Int
    )

    @GET("$SB_API_URL/api/branding/{videoId}")
    suspend fun getDeArrowContent(@Path("videoId") videoId: String): Map<String, DeArrowContent>

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
