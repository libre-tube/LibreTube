package com.github.libretube.ui.views

import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime

class TimePickerPreference(
    context: Context,
    attributeSet: AttributeSet
) : Preference(context, attributeSet) {
    override fun getSummary(): CharSequence {
        return PreferenceHelper.getString(key, DEFAULT_VALUE)
    }

    override fun onClick() {
        val prefTime = LocalTime.parse(PreferenceHelper.getString(key, DEFAULT_VALUE))
        val picker = MaterialTimePicker.Builder()
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setTimeFormat(timeFormat)
            .setHour(prefTime.hour)
            .setMinute(prefTime.minute)
            .build()

        picker.addOnPositiveButtonClickListener {
            val timeStr = LocalTime.of(picker.hour, picker.minute).toString()
            PreferenceHelper.putString(key, timeStr)
            summary = timeStr
        }
        picker.show((context as AppCompatActivity).supportFragmentManager, null)
    }

    private val timeFormat: Int
        get() = if (is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

    companion object {
        const val DEFAULT_VALUE = "12:00"
    }
}
