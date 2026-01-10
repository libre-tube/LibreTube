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
import com.google.android.material.chip.Chip

class SleepTimerSheet : ExpandedBottomSheet(R.layout.sleep_timer_sheet) {
    private var _binding: SleepTimerSheetBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = SleepTimerSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        setupQuickSelectChips()
        updateTimeLeftText()

        binding.startSleepTimer.setOnClickListener {
            val time = binding.timeInput.text.toString().toLongOrNull()

            if (time == null) {
                Toast.makeText(context, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            SleepTimer.start(requireContext(), time)
            updateTimeLeftText()
        }

        binding.stopSleepTimer.setOnClickListener {
            SleepTimer.stop(requireContext())
            updateTimeLeftText()
        }
    }

    /**
     * Setup quick-select chips for common sleep timer durations.
     */
    private fun setupQuickSelectChips() {
        val chipDurations = listOf(10, 20, 30, 45, 60)

        chipDurations.forEach { duration ->
            val chip = layoutInflater.inflate(
                R.layout.assist_chip,
                binding.quickSelectChips,
                false
            ) as Chip
            chip.apply {
                text = resources.getQuantityString(
                    R.plurals.sleep_timer_chip_minutes,
                    duration,
                    duration
                )
                setOnClickListener {
                    binding.timeInput.apply {
                        setText(duration.toString())
                        clearFocus()

                        SleepTimer.start(requireContext(), duration.toLong())
                        updateTimeLeftText()
                    }
                }
            }

            binding.quickSelectChips.addView(chip)
        }
    }

    private fun updateTimeLeftText() {
        val binding = _binding ?: return

        val isTimerRunning = SleepTimer.timeLeftMillis > 0

        binding.timeLeft.isVisible = isTimerRunning
        binding.stopSleepTimer.isVisible = isTimerRunning
        binding.timeInputLayout.isGone = isTimerRunning
        binding.quickSelectContainer.isGone = isTimerRunning
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
