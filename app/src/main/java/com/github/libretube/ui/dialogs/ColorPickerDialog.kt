package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.DialogColorPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ColorPickerDialog : DialogFragment(), SeekBar.OnSeekBarChangeListener {
    private var initialColor = 0

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialColor = requireArguments().getInt(IntentData.color)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogColorPickerBinding.inflate(layoutInflater)

        // Set initial color
        setColor(initialColor)

        binding.alphaSeekBar.setOnSeekBarChangeListener(this)
        binding.redSeekBar.setOnSeekBarChangeListener(this)
        binding.greenSeekBar.setOnSeekBarChangeListener(this)
        binding.blueSeekBar.setOnSeekBarChangeListener(this)

        // Add listener to text input
        binding.colorHexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            var isValid = true
            var oldHex = ""

            override fun afterTextChanged(s: Editable?) {
                // Update color when text input changes
                val hexColor = s.toString()
                if (hexColor.length == 9 && oldHex != hexColor) {
                    isValid = try {
                        oldHex = hexColor
                        val color = Color.parseColor(hexColor)
                        setColor(color, true)
                        true
                    } catch (e: IllegalArgumentException) {
                        if (isValid) {
                            showInvalidColorMessage()
                        }
                        false
                    }
                }
            }
        })

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.okay) { _, _ ->
                val color = getColor()
                setFragmentResult(
                    COLOR_PICKER_REQUEST_KEY,
                    bundleOf(IntentData.color to color)
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // Update color preview when SeekBar progress changes
        setColorPreview(getColor())
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        val newColorString = colorToString(getColor())

        if (newColorString != binding.colorHexInput.text.toString()) {
            binding.colorHexInput.setText(newColorString)
        }
    }

    private fun showInvalidColorMessage() {
        Toast.makeText(context, R.string.invalid_color, Toast.LENGTH_SHORT).show()
    }

    // Get the color from the SeekBar progress values
    private fun getColor() = Color.argb(
        binding.alphaSeekBar.progress,
        binding.redSeekBar.progress,
        binding.greenSeekBar.progress,
        binding.blueSeekBar.progress
    )

    private fun setColor(color: Int, textUpdate: Boolean = false) {
        // Set the SeekBar progress values based on the color
        binding.alphaSeekBar.progress = Color.alpha(color)
        binding.redSeekBar.progress = Color.red(color)
        binding.greenSeekBar.progress = Color.green(color)
        binding.blueSeekBar.progress = Color.blue(color)

        // Set the hex color input value
        if (!textUpdate) {
            binding.colorHexInput.setText(colorToString(color))
        }
        binding.colorPreview.setBackgroundColor(color)
    }

    private fun setColorPreview(color: Int) {
        // Set the color preview
        binding.colorPreview.setBackgroundColor(color)
    }

    private fun colorToString(color: Int): String {
        return String.format("#%08X", color)
    }

    companion object {
        const val COLOR_PICKER_REQUEST_KEY = "color_picker_request_key"
    }
}
