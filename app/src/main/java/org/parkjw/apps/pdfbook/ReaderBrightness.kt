package org.parkjw.apps.pdfbook

import android.view.WindowManager
import kotlin.math.roundToInt

const val READER_BRIGHTNESS_MIN_PERCENT = 1
const val READER_BRIGHTNESS_MAX_PERCENT = 100
const val DEFAULT_READER_BRIGHTNESS_PERCENT = 100

fun normalizeReaderBrightnessPercent(percent: Int): Int =
    percent.coerceIn(READER_BRIGHTNESS_MIN_PERCENT, READER_BRIGHTNESS_MAX_PERCENT)

fun isReaderBrightnessPercentValid(percent: Int?): Boolean =
    percent != null && percent in READER_BRIGHTNESS_MIN_PERCENT..READER_BRIGHTNESS_MAX_PERCENT

fun snapReaderBrightnessPercent(value: Float): Int {
    if (value.isNaN()) return DEFAULT_READER_BRIGHTNESS_PERCENT
    return normalizeReaderBrightnessPercent(value.roundToInt())
}

fun readerBrightnessOverride(followSystem: Boolean, percent: Int): Float {
    if (followSystem) return WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    return normalizeReaderBrightnessPercent(percent) / 100f
}
