package com.github.libretube.compat

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.core.app.RemoteActionCompat
import androidx.media3.common.VideoSize

class PictureInPictureParamsCompat private constructor(
    private val autoEnterEnabled: Boolean,
    private val seamlessResizeEnabled: Boolean,
    private val closeAction: RemoteActionCompat?,
    private val actions: List<RemoteActionCompat>,
    private val sourceRectHint: Rect?,
    private val title: CharSequence?,
    private val subtitle: CharSequence?,
    private val aspectRatio: Rational?,
    private val expandedAspectRatio: Rational?
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toPictureInPictureParams(): PictureInPictureParams {
        val pipParams = PictureInPictureParams.Builder()
            .setSourceRectHint(sourceRectHint)
            .setActions(actions.map { it.toRemoteAction() })
            .setAspectRatio(aspectRatio)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pipParams.setAutoEnterEnabled(autoEnterEnabled)
                .setSeamlessResizeEnabled(seamlessResizeEnabled)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pipParams.setTitle(title)
                .setSubtitle(subtitle)
                .setCloseAction(closeAction?.toRemoteAction())
                .setExpandedAspectRatio(expandedAspectRatio)
        }

        return pipParams.build()
    }

    class Builder {
        private var autoEnterEnabled = false
        private var seamlessResizeEnabled = true
        private var closeAction: RemoteActionCompat? = null
        private var actions: List<RemoteActionCompat> = emptyList()
        private var sourceRectHint: Rect? = null
        private var title: CharSequence? = null
        private var subtitle: CharSequence? = null
        private var aspectRatio: Rational? = null
        private var expandedAspectRatio: Rational? = null

        fun setAutoEnterEnabled(autoEnterEnabled: Boolean) = apply {
            this.autoEnterEnabled = autoEnterEnabled
        }

        fun setSeamlessResizeEnabled(seamlessResizeEnabled: Boolean) = apply {
            this.seamlessResizeEnabled = seamlessResizeEnabled
        }

        fun setCloseAction(action: RemoteActionCompat?) = apply {
            this.closeAction = action
        }

        fun setActions(actions: List<RemoteActionCompat>) = apply {
            this.actions = actions
        }

        fun setSourceRectHint(sourceRectHint: Rect?) = apply {
            this.sourceRectHint = sourceRectHint
        }

        fun setTitle(title: CharSequence?) = apply {
            this.title = title
        }

        fun setSubtitle(subtitle: CharSequence?) = apply {
            this.subtitle = subtitle
        }

        fun setAspectRatio(aspectRatio: Rational?) = apply {
            this.aspectRatio = aspectRatio
        }

        // Additional function replacing the project's extension function for the platform builder.
        fun setAspectRatio(videoSize: VideoSize): Builder {
            val ratio = (videoSize.width.toFloat() / videoSize.height)
            val rational = when {
                ratio.isNaN() -> Rational(4, 3)
                ratio <= 0.418410 -> Rational(41841, 100000)
                ratio >= 2.390000 -> Rational(239, 100)
                else -> Rational(videoSize.width, videoSize.height)
            }
            return setAspectRatio(rational)
        }

        fun setExpandedAspectRatio(expandedAspectRatio: Rational?) = apply {
            this.expandedAspectRatio = expandedAspectRatio
        }

        fun build(): PictureInPictureParamsCompat {
            return PictureInPictureParamsCompat(
                autoEnterEnabled,
                seamlessResizeEnabled,
                closeAction,
                actions,
                sourceRectHint,
                title,
                subtitle,
                aspectRatio,
                expandedAspectRatio
            )
        }
    }
}
