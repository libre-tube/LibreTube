package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.libretube.databinding.PlayerGestureControlViewBinding

class PlayerGestureControlView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    var binding: PlayerGestureControlViewBinding

    init {
        val layoutInflater = LayoutInflater.from(context)
        binding = PlayerGestureControlViewBinding.inflate(layoutInflater, this, true)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldHeight, oldHeight)

        binding.brightnessProgressBar.max = (height * 0.7).toInt()
        binding.volumeProgressBar.max = (height * 0.7).toInt()
    }
}
