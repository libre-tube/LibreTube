package com.github.libretube.ui.fragments

import android.os.Handler
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.github.libretube.helpers.PlayerHelper

class PlayerErrorHandler(
    private val handler: Handler,
    private val isAdded: () -> Boolean,
    private val isBindingValid: () -> Boolean,
    private val isPlayerInitialized: () -> Boolean,
) {
    private var errorRetryCount = 0

    fun createPlayerListener(
        playerController: MediaController,
        onError: () -> Unit
    ): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            val errorMsg = error.localizedMessage.orEmpty()

            if (PlayerHelper.isTransientPlayerError(error) && errorRetryCount < MAX_PLAYER_ERROR_RETRIES) {
                errorRetryCount++
                val delayMs = RETRY_BASE_DELAY_MS * errorRetryCount
                Log.w(TAG, "Transient player error (attempt $errorRetryCount/$MAX_PLAYER_ERROR_RETRIES): $errorMsg")
                handler.postDelayed({
                    if (!isAdded() || !isBindingValid() || !isPlayerInitialized()) return@postDelayed
                    try {
                        playerController.play()
                    } catch (e: Exception) {
                        Log.e(TAG, "Retry failed", e)
                    }
                }, delayMs)
            } else {
                Log.e(TAG, "Player error (non-retryable): $errorMsg", error)
                errorRetryCount = 0
            }
        }
    }

    fun reset() {
        errorRetryCount = 0
    }

    companion object {
        private const val TAG = "PlayerErrorHandler"
        private const val MAX_PLAYER_ERROR_RETRIES = 2
        private const val RETRY_BASE_DELAY_MS = 2000L
    }
}
