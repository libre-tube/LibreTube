package com.github.libretube.ui.views
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.github.libretube.R

class SbSpinnerPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private lateinit var spinner: Spinner
    private lateinit var adapter: ArrayAdapter<CharSequence>
    private var selectedItem: CharSequence? = null

    init {
        layoutResource = R.layout.spinner_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        spinner = holder.itemView.findViewById(R.id.spinner)

        val titleView = holder.itemView.findViewById<TextView>(android.R.id.title)
        titleView?.text = super.getTitle()

        val summaryView = holder.itemView.findViewById<TextView>(android.R.id.summary)
        summaryView?.text = super.getSummary()

        // Set the spinner adapter
        adapter = ArrayAdapter.createFromResource(
            context,
            R.array.sb_skip_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set the initial selected item
        if (selectedItem != null) {
            val position = adapter.getPosition(selectedItem!!)
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
                selectedItem = adapter.getItem(position)
                persistString(selectedItem?.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        // Get the default value from the XML attribute, if specified
        return a.getString(index) ?: ""
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        // Set the initial selected item from the persisted value, if available
        selectedItem = getPersistedString(defaultValue?.toString())
    }
}
