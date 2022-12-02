package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.libretube.databinding.PlayerGestureControlsViewBinding
import com.github.libretube.extensions.normalize

class PlayerGestureControlsView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    var binding: PlayerGestureControlsViewBinding

    init {
        val layoutInflater = LayoutInflater.from(context)
        binding = PlayerGestureControlsViewBinding.inflate(layoutInflater, this, true)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldHeight, oldHeight)

        // Set new max value of progress bar corresponding to the new height and
        // make progress accordingly, store oldProgress before changing it to avoid
        // inconsistency when old progress > new max
        binding.brightnessProgressBar.apply {
            val oldMax = max
            val oldProgress = progress
            max = (height * 0.7).toInt()
            progress = oldProgress.normalize(0, oldMax, 0, max)
        }

        binding.volumeProgressBar.apply {
            val oldMax = max
            val oldProgress = progress
            max = (height * 0.7).toInt()
            progress = oldProgress.normalize(0, oldMax, 0, max)
        }
    }
}
