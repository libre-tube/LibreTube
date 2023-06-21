package com.github.libretube.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.libretube.R
import com.github.libretube.ui.dialogs.ColorPickerDialog

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
        } else {
            getPersistedInt(Color.WHITE)
        }
    }

    private fun updateColorView() {
        (if (currentColor is Int) currentColor else Color.WHITE)?.let {
            circleView.setBackgroundColor(it)
        }
    }

    private fun showColorPickerDialog() {
        (if (currentColor is Int) currentColor else Color.BLACK)?.let {
            val dialog = ColorPickerDialog(
                context,
                it,
                object : ColorPickerDialog.OnColorSelectedListener {
                    override fun onColorSelected(color: Int) {
                        setColor(color)
                    }
                })
            dialog.show((context as AppCompatActivity).supportFragmentManager, this::class.java.name)
        }
    }

    override fun getTitle(): CharSequence? {
        return "${super.getTitle()}:"
    }

}
