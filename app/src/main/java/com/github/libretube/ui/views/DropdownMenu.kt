package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.github.libretube.R
import com.github.libretube.databinding.DropdownMenuBinding

/**
 * Exposed Dropdown Menu
 */
class DropdownMenu(
    context: Context,
    attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {
    var binding: DropdownMenuBinding

    @Suppress("UNCHECKED_CAST")
    var adapter: ArrayAdapter<String>
        get() = binding.autoCompleteTextView.adapter as ArrayAdapter<String>
        set(value) {
            binding.autoCompleteTextView.setAdapter(value)
            binding.autoCompleteTextView.setText(value.getItem(0), false)
        }

    val selectedItemPosition: Int get() = adapter.getPosition(
        binding.autoCompleteTextView.text.toString()
    )

    init {
        val layoutInflater = LayoutInflater.from(context)
        binding = DropdownMenuBinding.inflate(layoutInflater, this, true)

        val ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.DropdownMenu, 0, 0)

        try {
            binding.textInputLayout.hint = ta.getString(R.styleable.DropdownMenu_hint)
            binding.textInputLayout.startIconDrawable = ta.getDrawable(
                R.styleable.DropdownMenu_icon
            )
        } finally {
            ta.recycle()
        }
    }

    fun setSelection(index: Int) {
        binding.autoCompleteTextView.setText(adapter.getItem(index))
    }
}
