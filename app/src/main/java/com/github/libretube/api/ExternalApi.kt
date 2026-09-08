package com.github.libretube.api

import com.github.libretube.api.obj.DeArrowBody
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.PipedConfig
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.SubmitSegmentResponse
import com.github.libretube.api.obj.VideoLabelData
import com.github.libretube.api.obj.VoteInfo
import com.github.libretube.constants.ApiConstants
import com.github.libretube.obj.update.UpdateInfo
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ExternalApi {
    @GET("config")
    suspend fun getInstanceConfig(@Url url: String): PipedConfig

    @GET(ApiConstants.GITHUB_API_URL)
    suspend fun getLatestRelease(): UpdateInfo

    @GET("${ApiConstants.RYD_API_URL}/votes")
    suspend fun getVotes(@Query("videoId") videoId: String): VoteInfo

    @POST("${ApiConstants.SPONSORBLOCK_API_URL}/api/skipSegments")
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

    @GET("${ApiConstants.SPONSORBLOCK_API_URL}/api/skipSegments/{videoId}")
    suspend fun getSegments(
        @Path("videoId") videoId: String,
        @Query("category") category: List<String>,
        @Query("actionType") actionType: List<String>? = null
    ): List<SegmentData>

    @GET("${ApiConstants.SPONSORBLOCK_API_URL}/api/videoLabels/{videoId}")
    suspend fun getVideoLabels(
        @Path("videoId") videoId: String,
    ): List<VideoLabelData>

    @POST("${ApiConstants.SPONSORBLOCK_API_URL}/api/branding")
    suspend fun submitDeArrow(@Body body: DeArrowBody)

    @POST("${ApiConstants.SPONSORBLOCK_API_URL}/api/voteOnSponsorTime")
    suspend fun voteOnSponsorTime(
        @Query("UUID") uuid: String,
        @Query("userID") userID: String,
        @Query("type") score: Int
    )

    @GET("${ApiConstants.SPONSORBLOCK_API_URL}/api/branding/{videoId}")
    suspend fun getDeArrowContent(@Path("videoId") videoId: String): Map<String, DeArrowContent>

    @Headers(
        "User-Agent: ${ApiConstants.USER_AGENT}",
        "Accept: application/json",
        "Content-Type: application/json+protobuf",
        "x-goog-api-key: ${ApiConstants.GOOGLE_API_KEY}",
        "x-user-agent: grpc-web-javascript/0.1",
    )
    @POST
    suspend fun botguardRequest(
        @Url url: String,
        @Body jsonPayload: List<String>
    ): JsonElement
}
