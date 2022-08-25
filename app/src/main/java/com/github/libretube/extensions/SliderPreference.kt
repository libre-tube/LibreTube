package com.github.libretube.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.databinding.DialogSliderBinding
import com.github.libretube.preferences.PreferenceHelper
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
    override fun onClick() {
        val sliderBinding = DialogSliderBinding.inflate(
            LayoutInflater.from(context)
        )
        sliderBinding.slider.value = PreferenceHelper.getString(
            key,
            "1.0"
        ).toFloat()

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
