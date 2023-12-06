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
) : Preference(context, attributeSet) {
    private lateinit var sliderBinding: DialogSliderBinding
    private val defValue: Float
    private val valueFrom: Float
    private val valueTo: Float
    private val stepSize: Float

    init {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.SliderPreference)
        defValue = ta.getFloat(R.styleable.SliderPreference_defValue, 0f)
        valueFrom = ta.getFloat(R.styleable.SliderPreference_valueFrom, 1f)
        valueTo = ta.getFloat(R.styleable.SliderPreference_valueTo, 10f)
        stepSize = ta.getFloat(R.styleable.SliderPreference_stepSize, 1f)
        ta.recycle()
    }

    private var prefValue: Float
        get() = PreferenceHelper.getString(key, defValue.toString()).toFloat()
        set(value) = PreferenceHelper.putString(key, value.toString())

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
