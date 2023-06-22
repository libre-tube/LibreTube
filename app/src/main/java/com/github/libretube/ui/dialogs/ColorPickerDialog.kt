package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.github.libretube.R
import com.github.libretube.databinding.DialogColorPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ColorPickerDialog(
    private val context: Context,
    private val initialColor: Int,
    private val onColorSelectedListener: OnColorSelectedListener
) : DialogFragment(), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogColorPickerBinding.inflate(layoutInflater)

        // Set initial color
        setColor(initialColor)

        binding.alphaSeekBar.setOnSeekBarChangeListener(this)
        binding.redSeekBar.setOnSeekBarChangeListener(this)
        binding.greenSeekBar.setOnSeekBarChangeListener(this)
        binding.blueSeekBar.setOnSeekBarChangeListener(this)
        binding.okay.setOnClickListener(this)
        binding.cancel.setOnClickListener(this)

        //Add listener to textbox
        binding.colorHexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int,
                                           after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int,
                                       count: Int) = Unit

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

    override fun onClick(v: View?) {
        if (v?.id == R.id.okay) {
            onColorSelectedListener.onColorSelected(getColor())
            dismiss()
        } else if (v?.id == R.id.cancel) {
            dismiss()
        }
    }

    private fun getColor(): Int {
        // Get the color from the SeekBar progress values
        return Color.argb(binding.alphaSeekBar.progress, binding.redSeekBar.progress,
            binding.greenSeekBar.progress, binding.blueSeekBar.progress)
    }

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

    fun interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }
}
