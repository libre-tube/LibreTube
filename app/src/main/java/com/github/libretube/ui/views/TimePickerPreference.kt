package com.github.libretube.ui.views

import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.TextUtils
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class TimePickerPreference(
    context: Context,
    attributeSet: AttributeSet
) : Preference(context, attributeSet) {
    override fun getSummary(): CharSequence {
        val prefStr = PreferenceHelper.getString(key, "")
        return if (prefStr != "") prefStr else DEFAULT_VALUE
    }

    override fun onClick() {
        val picker = MaterialTimePicker.Builder()
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setTimeFormat(getTimeFormat())
            .setHour(getHour())
            .setMinute(getMinutes())
            .build()

        picker.addOnPositiveButtonClickListener {
            val timeStr = getTimeStr(picker)
            PreferenceHelper.putString(key, timeStr)
            summary = timeStr
        }
        picker.show((context as AppCompatActivity).supportFragmentManager, null)
    }

    private fun getTimeFormat(): Int {
        return if (is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
    }

    private fun getPrefStringPart(index: Int): String? {
        val prefStr = PreferenceHelper.getString(key, "").split(SEPARATOR).getOrNull(index)
        return if (prefStr != "") prefStr else null
    }

    private fun getHour(): Int {
        return getPrefStringPart(0)?.toInt() ?: 0
    }

    private fun getMinutes(): Int {
        return getPrefStringPart(1)?.toInt() ?: 0
    }

    private fun getTimeStr(picker: MaterialTimePicker): String {
        val hour = TextUtils.toTwoDecimalsString(picker.hour)
        val minute = TextUtils.toTwoDecimalsString(picker.minute)
        return "$hour$SEPARATOR$minute"
    }

    companion object {
        const val SEPARATOR = ":"
        const val DEFAULT_VALUE = "12:00"
    }
}
