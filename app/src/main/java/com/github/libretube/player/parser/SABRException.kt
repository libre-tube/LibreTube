package com.github.libretube.player.parser

import java.io.IOException

/**
 * Exception specific to SABR (Server Adaptive Bitrate) streaming errors.
 *
 * Contains the SABR error type and code from the server response.
 * This allows for more specific error handling and user-facing messages.
 */
class SABRException(
    val sabrErrorType: String,
    val sabrErrorCode: Int,
) : IOException("SABR error: $sabrErrorType (code: $sabrErrorCode)") {

    val userFacingMessage: String
        get() = when (sabrErrorType) {
            "sabr.no_video_selected" -> "No video stream available for this content."
            "sabr.playback_start_policy" -> "Playback was rejected by the server. Retrying…"
            "sabr.content_not_available" -> "This content is not available in your region."
            "sabr.age_restricted" -> "This content is age-restricted."
            "sabr.private_video" -> "This video is private."
            "sabr.unavailable" -> "This video is unavailable."
            "sabr.error" -> "A streaming error occurred."
            else -> "Streaming error: $sabrErrorType"
        }

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * A fatal, non-retryable SABR error (e.g. the server asked the player to reload).
 *
 * It carries no useful retry semantics - re-issuing the same request would return the same
 * response - so the player must surface the error instead of looping through load retries.
 */
class SabrFatalException(message: String) : IOException(message)
