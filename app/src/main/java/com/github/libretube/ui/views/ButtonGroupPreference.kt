package com.github.libretube.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.libretube.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class ButtonGroupPreference(context: Context, val attrs: AttributeSet?): Preference(context, attrs) {
    private lateinit var entries: Array<CharSequence>
    private lateinit var entryValues: Array<CharSequence>

    init {
        layoutResource = R.layout.preference_button_group


        // obtain custom style attributes
        context.withStyledAttributes(attrs, R.styleable.ButtonGroupPreference, 0, 0) {
            entries = getTextArray(R.styleable.ButtonGroupPreference_optionEntries)!!
            entryValues = getTextArray(R.styleable.ButtonGroupPreference_optionValues)!!

            assert(entries.size == entryValues.size)
        }
    }

    private var defaultValue = ""
    val value get() = getPersistedString(defaultValue)!!

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val view = holder.itemView

        // default preference stuff
        view.findViewById<TextView>(android.R.id.title).text = title
        view.findViewById<ImageView>(android.R.id.icon).setImageDrawable(icon)

        val buttonGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.button_group)
        buttonGroup.removeAllViews()

        // otherwise the buttonGroup.check below triggers the listener
        buttonGroup.clearOnButtonCheckedListeners()

        // add one button for each option to the view
        for ((title, v) in entries.zip(entryValues)) {
            // custom button styling is tricky, see https://stackoverflow.com/questions/60590968/set-materialbutton-style-to-textbutton-programmatically
            val button = MaterialButton(context, null, R.attr.tonalButtonStyleCustomAttr).also {
                it.id = View.generateViewId()
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                it.text = title
            }
            buttonGroup.addView(button)

            // calling MaterialButton#setChecked breaks everything, we have to call
            // MaterialButtonGroup#check instead so that the button group works properly
            if (value == v) buttonGroup.check(button.id)
        }

        buttonGroup.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val i = buttonGroup.children.indexOfFirst { it.id == id }
            val newValue = entryValues[i].toString()

            val confirmed = onPreferenceChangeListener?.onPreferenceChange(this, newValue) != false
            if (confirmed) {
                persistString(newValue)
            }
        }
    }

    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        // Get the default value from the XML attribute, if specified
        defaultValue = ta.getString(index).orEmpty()
        return defaultValue
    }
}