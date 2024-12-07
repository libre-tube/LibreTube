package com.github.libretube.ui.extensions

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

private val GestureInterpolator = PathInterpolatorCompat.create(0f, 0f, 0f, 1f)

/**
 * Set a fragment animation to be displayed when swiping back as well as an [onBackPressed] action
 * that is executed when the back press was confirmed and not cancelled by the user
 *
 * @see <a href="https://github.com/android/animation-samples/blob/main/Motion/app/src/main/java/com/example/android/motion/demo/containertransform/CheeseArticleFragment.kt">Android animation samples</a>
 */
fun Fragment.setupFragmentAnimation(
    background: View, onBackPressed: () -> Unit = {
        findNavController().popBackStack()
    }
) {
    val predictiveBackMargin = 50f
    var initialTouchY = -1f

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            val progress = GestureInterpolator.getInterpolation(backEvent.progress)
            if (initialTouchY < 0f) {
                initialTouchY = backEvent.touchY
            }
            val progressY = GestureInterpolator.getInterpolation(
                (backEvent.touchY - initialTouchY) / background.height
            )

            // See the motion spec about the calculations below.
            // https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs

            // Shift horizontally.
            val maxTranslationX = (background.width / 20) - predictiveBackMargin
            background.translationX = progress * maxTranslationX *
                    (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

            // Shift vertically.
            val maxTranslationY = (background.height / 20) - predictiveBackMargin
            background.translationY = progressY * maxTranslationY

            // Scale down from 100% to 90%.
            val scale = 1f - (0.1f * progress)
            background.scaleX = scale
            background.scaleY = scale
        }

        override fun handleOnBackCancelled() {
            initialTouchY = -1f
            background.run {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
            }
        }
    }

    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
}