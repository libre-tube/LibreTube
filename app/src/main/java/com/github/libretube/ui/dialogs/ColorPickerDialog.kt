package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import com.github.libretube.R

class ColorPickerDialog(
    context: Context,
    private val initialColor: Int,
    private val onColorSelectedListener: OnColorSelectedListener
) : Dialog(context), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private lateinit var colorPreview: View
    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar
    private lateinit var alphaSeekBar: SeekBar
    private lateinit var colorHexInput: EditText
    private lateinit var okay: Button
    private lateinit var cancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_picker)

        // Initialize UI elements
        colorPreview = findViewById(R.id.colorPreview)
        redSeekBar = findViewById(R.id.redSeekBar)
        greenSeekBar = findViewById(R.id.greenSeekBar)
        blueSeekBar = findViewById(R.id.blueSeekBar)
        alphaSeekBar = findViewById(R.id.alphaSeekBar)
        colorHexInput = findViewById(R.id.colorHexInput)
        okay = findViewById(R.id.okay)
        cancel = findViewById(R.id.cancel)

        // Set initial color
        setColor(initialColor)

        redSeekBar.setOnSeekBarChangeListener(this)
        greenSeekBar.setOnSeekBarChangeListener(this)
        blueSeekBar.setOnSeekBarChangeListener(this)
        alphaSeekBar.setOnSeekBarChangeListener(this)
        okay.setOnClickListener(this)
        cancel.setOnClickListener(this)


        colorHexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            var valid = true
            var oldHex = ""
            override fun afterTextChanged(s: Editable?) {
                // Update color when text input changes
                val hexColor = s.toString()
                if (hexColor.length == 9 && oldHex != hexColor) {
                    valid = try {
                        oldHex = hexColor
                        val color = Color.parseColor(hexColor)
                        setColor(color, true)
                        true
                    } catch (e: IllegalArgumentException) {
                        if (valid) {
                            showInvalidColorMessage()
                        }
                        false
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val dialogWidth = ViewGroup.LayoutParams.MATCH_PARENT
        val dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT
        window?.setLayout(dialogWidth, dialogHeight)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        // Update color preview when SeekBar progress changes
        val color = getColor()
        setColorPreview(color)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        val newColorString = colorToString(getColor())

        if (newColorString != colorHexInput.text.toString()) {
            colorHexInput.setText(newColorString)
        }
    }


    private fun showInvalidColorMessage() {
        val invalidColorMessage = R.string.invalid_color
        Toast.makeText(context, invalidColorMessage, Toast.LENGTH_SHORT).show()
    }


    override fun onClick(v: View?) {
        if (v?.id == R.id.okay) {
            // Notify the selected color
            val color = getColor()
            onColorSelectedListener.onColorSelected(color)
            dismiss()
        } else if (v?.id == R.id.cancel) {
            dismiss()
        }
    }

    private fun getColor(): Int {
        // Get the color from the SeekBar progress values
        val red = redSeekBar.progress
        val green = greenSeekBar.progress
        val blue = blueSeekBar.progress
        val alpha = alphaSeekBar.progress
        return Color.argb(alpha, red, green, blue)
    }

    private fun setColor(color: Int, textUpdate: Boolean = false) {
        // Set the SeekBar progress values based on the color
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val alpha = Color.alpha(color)
        redSeekBar.progress = red
        greenSeekBar.progress = green
        blueSeekBar.progress = blue
        alphaSeekBar.progress = alpha

        // Set the hex color input value
        if (!textUpdate) {
            colorHexInput.setText(colorToString(color))
        }
        colorPreview.setBackgroundColor(color)
    }

    private fun setColorPreview(color: Int) {
        // Set the color preview
        colorPreview.setBackgroundColor(color)
    }

    private fun colorToString(color: Int): String {
        return String.format("#%08X", color)
    }

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }


}