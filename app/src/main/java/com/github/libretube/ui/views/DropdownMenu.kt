package com.github.libretube.ui.views

import android.content.Context
import android.text.InputType
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
        private set(value) {
            binding.autoCompleteTextView.setAdapter(value)
            if (!value.isEmpty) binding.autoCompleteTextView.setText(value.getItem(0), false)
        }

    var items: List<String>
        get() = (0 until adapter.count).mapNotNull { adapter.getItem(it) }
        set(value) {
            adapter = ArrayAdapter(context, R.layout.dropdown_item, value)
        }

    var selectedItemPosition: Int
        get() = adapter.getPosition(binding.autoCompleteTextView.text.toString())
        set(index) = binding.autoCompleteTextView.setText(adapter.getItem(index), false)

    val selectedItem get() = binding.autoCompleteTextView.text.toString()

    var typingEnabled: Boolean
        set(enabled) {
            binding.autoCompleteTextView.inputType = if (enabled) InputType.TYPE_CLASS_TEXT else InputType.TYPE_NULL
        }
        get() = binding.autoCompleteTextView.inputType != InputType.TYPE_NULL

    override fun setEnabled(enabled: Boolean) {
        binding.textInputLayout.isEnabled = enabled
    }

    override fun isEnabled() = binding.textInputLayout.isEnabled

    fun setSelection(item: String) {
        val itemIndex = items.indexOf(item)
        if (itemIndex != -1) selectedItemPosition = itemIndex
    }

    fun getSelectionIfNotFirst(): String? {
        return selectedItem.takeIf { selectedItemPosition != 0 }
    }

    init {
        context.obtainStyledAttributes(attributeSet, R.styleable.DropdownMenu, 0, 0).use {
            binding.textInputLayout.hint = it.getString(R.styleable.DropdownMenu_hint)
            binding.textInputLayout.startIconDrawable =
                it.getDrawable(R.styleable.DropdownMenu_icon)
        }

        adapter = ArrayAdapter(context, R.layout.dropdown_item)
    }
}
