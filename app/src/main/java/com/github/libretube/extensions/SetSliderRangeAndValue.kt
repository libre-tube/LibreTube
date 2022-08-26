package com.github.libretube.extensions

import com.github.libretube.obj.SliderRange
import com.google.android.material.slider.Slider

/**
 * set the range of the slider preference
 */
fun Slider.setSliderRangeAndValue(range: SliderRange) {
    this.valueFrom = range.valueFrom
    this.valueTo = range.valueTo
    this.stepSize = range.stepSize
    this.value = range.defaultValue
}
