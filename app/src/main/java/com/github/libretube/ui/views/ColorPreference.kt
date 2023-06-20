package com.github.libretube.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.libretube.R

class ColorPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private lateinit var circleView: View
    private var currentColor: Int? = null

    init {
        layoutResource = R.layout.color_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.findViewById<TextView>(android.R.id.title)?.text = getTitle()
        circleView = holder.itemView.findViewById(R.id.circle)
        updateColorView()

        circleView.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun setColor(color: Int) {
        currentColor = color
        persistInt(color)
        updateColorView()
    }


    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        return Color.parseColor(ta.getString(index))
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentColor = if (defaultValue is Int) {
            getPersistedInt(defaultValue)
        } else{
            getPersistedInt(Color.WHITE)
        }
    }

    private fun updateColorView() {
        (if (currentColor is Int) currentColor else Color.WHITE)?.let {
            circleView.setBackgroundColor(
                it
            )
        }
    }

    private fun showColorPickerDialog() {
        val colorEditText = EditText(context)
        val dialog = android.app.AlertDialog.Builder(context)
            .setTitle(R.string.enter_hex_value)
            .setView(colorEditText)
            .setPositiveButton(R.string.okay) { _, _ ->
                var hexValue = colorEditText.text.toString().trim()
                if (!hexValue.startsWith('#')) {
                    hexValue = "#$hexValue"
                }

                if (hexValue.isNotEmpty()) {
                    try {
                        val color = Color.parseColor(hexValue)
                        setColor(color)
                    }
                    catch (e: IllegalArgumentException){
                        showInvalidColorMessage()
                    }

                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showInvalidColorMessage() {
        val invalidColorMessage = R.string.invalid_color
        Toast.makeText(context, invalidColorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun getTitle(): CharSequence? {
        return "${super.getTitle()}:"
    }


}
