package com.github.libretube.player.manifest

import androidx.media3.common.util.UnstableApi

/**
 * A base URL, as defined by ISO 23009-1, 2nd edition, 5.6. and ETSI TS 103 285 V1.2.1, 10.8.2.1
 */
@UnstableApi
data class BaseUrl(
    val url: String,
    val serviceLocation: String = url,
    val priority: Int = PRIORITY_UNSET,
    val weight: Int = DEFAULT_WEIGHT
) {
    companion object {
        const val DEFAULT_WEIGHT = 1
        const val DEFAULT_DVB_PRIORITY = 1
        const val PRIORITY_UNSET = Int.MIN_VALUE
    }
}
