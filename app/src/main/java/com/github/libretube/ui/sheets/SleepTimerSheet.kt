package com.github.libretube.ui.sheets

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.github.libretube.R
import com.github.libretube.databinding.SleepTimerSheetBinding
import com.github.libretube.ui.tools.SleepTimer

class SleepTimerSheet : ExpandedBottomSheet(R.layout.sleep_timer_sheet) {
    private var _binding: SleepTimerSheetBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SleepTimerSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        updateTimeLeftText()

        binding.startSleepTimer.setOnClickListener {
            val time = binding.timeInput.text.toString().toLongOrNull()

            if (time == null) {
                Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SleepTimer.setup(requireContext(), time)

            updateTimeLeftText()
        }

        binding.stopSleepTimer.setOnClickListener {
            SleepTimer.disableSleepTimer()
            updateTimeLeftText()
        }
    }

    private fun updateTimeLeftText() {
        val binding = _binding ?: return

        val isTimerRunning = SleepTimer.timeLeftMillis > 0

        binding.timeLeft.isVisible = isTimerRunning
        binding.stopSleepTimer.isVisible = isTimerRunning
        binding.timeInputLayout.isGone = isTimerRunning
        binding.startSleepTimer.isGone = isTimerRunning

        if (!isTimerRunning) return

        binding.timeLeft.text = DateUtils.formatElapsedTime(SleepTimer.timeLeftMillis / 1000)

        handler.postDelayed(1000) {
            updateTimeLeftText()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
