package com.github.libretube.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.github.libretube.helpers.PlayerHelper

@UnstableApi
class OfflinePlayerViewModel(
    val player: ExoPlayer,
) : ViewModel() {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[APPLICATION_KEY]!!
                val trackSelector = DefaultTrackSelector(context)
                OfflinePlayerViewModel(
                    player = PlayerHelper.createPlayer(context, trackSelector, false),
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}