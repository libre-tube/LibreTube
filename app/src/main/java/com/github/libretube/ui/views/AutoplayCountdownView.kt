package com.github.libretube.ui.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.postDelayed
import com.github.libretube.R
import com.github.libretube.databinding.AutoplayCountdownBinding

class AutoplayCountdownView(
    context: Context,
    attributeSet: AttributeSet?,
) : FrameLayout(context, attributeSet) {
    private val layoutInflater = LayoutInflater.from(context)
    val binding = AutoplayCountdownBinding.inflate(layoutInflater, this, true)
    private var onCountdownEnd: () -> Unit = {}
    private var hideSelf: () -> Unit = {}
    private val handler = Handler(Looper.getMainLooper())
    private var currentTimerState = COUNTDOWN_SECONDS

    init {
        binding.cancel.setOnClickListener {
            handler.removeCallbacksAndMessages(TIMER_RUNNABLE_TOKEN)
            hideSelf.invoke()
        }
    }

    fun setHideSelfListener(listener: () -> Unit) {
        hideSelf = listener
    }

    fun startCountdown(onEnd: () -> Unit) {
        this.visibility = View.VISIBLE
        onCountdownEnd = {
            hideSelf.invoke()
            onEnd.invoke()
        }
        currentTimerState = COUNTDOWN_SECONDS
        binding.playNext.setOnClickListener {
            handler.removeCallbacksAndMessages(TIMER_RUNNABLE_TOKEN)
            onCountdownEnd.invoke()
        }
        updateCountdown()
    }

    private fun updateCountdown() {
        if (currentTimerState == 0) {
            onCountdownEnd.invoke()
            return
        }

        binding.currentState.text = context.getString(
            R.string.playing_next,
            currentTimerState.toString(),
        )
        currentTimerState -= 1
        handler.postDelayed(1000, TIMER_RUNNABLE_TOKEN, this::updateCountdown)
    }

    companion object {
        private const val COUNTDOWN_SECONDS = 5
        private const val TIMER_RUNNABLE_TOKEN = "timer_runnable"
    }
}
