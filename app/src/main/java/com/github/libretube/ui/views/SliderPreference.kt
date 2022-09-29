package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.databinding.DialogSliderBinding
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Preference that includes a slider
 */
class SliderPreference(
    context: Context,
    attributeSet: AttributeSet
) : Preference(
    context,
    attributeSet
) {
    private lateinit var sliderBinding: DialogSliderBinding

    val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.SliderPreference)

    override fun onClick() {
        val defValue = typedArray.getFloat(R.styleable.SliderPreference_defValue, 1.0f)
        val valueFrom = typedArray.getFloat(R.styleable.SliderPreference_valueFrom, 1.0f)
        val valueTo = typedArray.getFloat(R.styleable.SliderPreference_valueTo, 10.0f)
        val stepSize = typedArray.getFloat(R.styleable.SliderPreference_stepSize, 1.0f)

        sliderBinding = DialogSliderBinding.inflate(
            LayoutInflater.from(context)
        )

        sliderBinding.slider.apply {
            value = PreferenceHelper.getString(
                key,
                defValue.toString()
            ).toFloat()
            this.valueFrom = valueFrom
            this.valueTo = valueTo
            this.stepSize = stepSize
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(sliderBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                PreferenceHelper.putString(
                    key,
                    sliderBinding.slider.value.toString()
                )
            }
            .show()
        super.onClick()
    }
}
