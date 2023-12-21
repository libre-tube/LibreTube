package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.core.content.res.use
import com.github.libretube.R
import com.github.libretube.databinding.DropdownMenuBinding

/**
 * Exposed Dropdown Menu
 */
class DropdownMenu(
    context: Context,
    attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {
    private val binding =
        DropdownMenuBinding.inflate(LayoutInflater.from(context), this, true)

    @Suppress("UNCHECKED_CAST")
    var adapter: ArrayAdapter<String>
        get() = binding.autoCompleteTextView.adapter as ArrayAdapter<String>
        set(value) {
            binding.autoCompleteTextView.setAdapter(value)
            if (!value.isEmpty) binding.autoCompleteTextView.setText(value.getItem(0), false)
        }

    val selectedItemPosition: Int
        get() = adapter.getPosition(binding.autoCompleteTextView.text.toString())

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.DropdownMenu, 0, 0).use {
            binding.textInputLayout.hint = it.getString(R.styleable.DropdownMenu_hint)
            binding.textInputLayout.startIconDrawable =
                it.getDrawable(R.styleable.DropdownMenu_icon)
        }

        adapter = ArrayAdapter(context, R.layout.dropdown_item)
    }

    fun setItems(items: List<String>) {
        adapter = ArrayAdapter(context, R.layout.dropdown_item, items)
    }

    fun setSelection(index: Int) {
        binding.autoCompleteTextView.setText(adapter.getItem(index), false)
    }
}
