package com.github.libretube.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.databinding.DialogSliderBinding
import com.github.libretube.extensions.round
import com.github.libretube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

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
    private var defValue = 0f
    private var valueFrom = 1f
    private var valueTo = 10f
    private var stepSize = 1f

    private var prefValue: Float
        get() = PreferenceHelper.getString(
            key,
            defValue.toString()
        ).toFloat()
        set(value) {
            PreferenceHelper.putString(
                key,
                value.toString()
            )
        }

    private val typedArray = context.obtainStyledAttributes(
        attributeSet,
        R.styleable.SliderPreference
    )

    override fun onAttached() {
        super.onAttached()

        defValue = typedArray.getFloat(R.styleable.SliderPreference_defValue, defValue)
        valueFrom = typedArray.getFloat(R.styleable.SliderPreference_valueFrom, valueFrom)
        valueTo = typedArray.getFloat(R.styleable.SliderPreference_valueTo, valueTo)
        stepSize = typedArray.getFloat(R.styleable.SliderPreference_stepSize, stepSize)
    }

    override fun getSummary(): CharSequence = getDisplayedCurrentValue(prefValue)

    override fun onClick() {
        sliderBinding = DialogSliderBinding.inflate(
            LayoutInflater.from(context)
        )

        sliderBinding.slider.apply {
            value = prefValue
            valueFrom = this@SliderPreference.valueFrom
            valueTo = this@SliderPreference.valueTo
            stepSize = this@SliderPreference.stepSize
        }

        sliderBinding.minus.setOnClickListener {
            sliderBinding.slider.value = maxOf(valueFrom, sliderBinding.slider.value - stepSize)
        }

        sliderBinding.plus.setOnClickListener {
            sliderBinding.slider.value = minOf(valueTo, sliderBinding.slider.value + stepSize)
        }

        sliderBinding.slider.addOnChangeListener { slider, _, _ ->
            listOf(sliderBinding.minus, sliderBinding.plus).forEach {
                it.alpha = 1f
            }
            when (slider.value) {
                slider.valueFrom -> sliderBinding.minus.alpha = 0.5f
                slider.valueTo -> sliderBinding.plus.alpha = 0.5f
            }
            updateCurrentValueText()
        }

        updateCurrentValueText()

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(sliderBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay) { _, _ ->
                prefValue = sliderBinding.slider.value
                summary = sliderBinding.slider.value.toString()
            }
            .show()
        super.onClick()
    }

    private fun updateCurrentValueText() {
        sliderBinding.currentValue.text = getDisplayedCurrentValue(sliderBinding.slider.value)
    }

    private fun getDisplayedCurrentValue(currentValue: Float): String {
        // if the preference only accepts integer steps, we don't need to show the decimals,
        // as these decimals are just zero and hence not useful for the user
        if (valueTo % 1 == 0f && valueFrom % 1 == 0f && stepSize % 1 == 0f) {
            return currentValue.roundToInt().toString()
        }
        return currentValue.round(2).toString()
    }
}
