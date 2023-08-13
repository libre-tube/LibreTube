package com.github.libretube.ui.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.libretube.R
import com.github.libretube.helpers.PreferenceHelper

class SbSpinnerPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private lateinit var adapter: ArrayAdapter<CharSequence>
    private lateinit var initialValue: String

    init {
        layoutResource = R.layout.spinner_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val spinner: Spinner = holder.itemView.findViewById(R.id.spinner)

        holder.itemView.findViewById<TextView>(android.R.id.title)?.text = super.getTitle()
        holder.itemView.findViewById<TextView>(android.R.id.summary)?.text = super.getSummary()

        // Set the spinner adapter
        adapter = ArrayAdapter.createFromResource(
            context,
            R.array.sb_skip_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = null

        // Set the initial selected item
        PreferenceHelper.getString(key, initialValue).let { selectedItem ->
            val position = getEntryValues().indexOf(selectedItem)
            spinner.setSelection(position)
        }

        // Set a listener to handle item selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newValue = getEntryValues()[position]
                persistString(newValue)
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        // Get the default value from the XML attribute, if specified
        val defaultValue = ta.getString(index).orEmpty()
        this.initialValue = defaultValue
        return defaultValue
    }

    private fun getEntryValues() = context.resources.getStringArray(R.array.sb_skip_options_values)
}
