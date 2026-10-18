package org.parkjw.apps.pdfbook

import kotlin.math.roundToInt

enum class TapZone(val pageDelta: Int) {
    Previous(-1),
    Menu(0),
    Next(1),
}

data class TapZoneSpan(
    val zone: TapZone,
    val start: Float,
    val end: Float,
) {
    val width: Float get() = end - start
}

val TapZone.acceptsDoubleTapAtBaseScale: Boolean
    get() = this == TapZone.Menu

const val DEFAULT_TAP_ZONE_SIDE_FRACTION = 1f / 3f
const val TAP_ZONE_SIDE_MIN_PERCENT = 20
const val TAP_ZONE_SIDE_MAX_PERCENT = 45
const val TAP_ZONE_SIDE_STEP_PERCENT = 5
const val DEFAULT_TAP_ZONE_SIDE_PERCENT = 33

fun normalizeTapZoneSidePercent(percent: Int): Int {
    if (percent == DEFAULT_TAP_ZONE_SIDE_PERCENT) return percent
    return snapTapZoneSidePercent(percent.toFloat())
}

fun snapTapZoneSidePercent(value: Float): Int {
    if (value.isNaN()) return DEFAULT_TAP_ZONE_SIDE_PERCENT
    val clamped = value.coerceIn(TAP_ZONE_SIDE_MIN_PERCENT.toFloat(), TAP_ZONE_SIDE_MAX_PERCENT.toFloat())
    val steps = ((clamped - TAP_ZONE_SIDE_MIN_PERCENT) / TAP_ZONE_SIDE_STEP_PERCENT).roundToInt()
    return TAP_ZONE_SIDE_MIN_PERCENT + steps * TAP_ZONE_SIDE_STEP_PERCENT
}

fun tapZoneSideFraction(percent: Int): Float {
    val normalized = normalizeTapZoneSidePercent(percent)
    if (normalized == DEFAULT_TAP_ZONE_SIDE_PERCENT) return DEFAULT_TAP_ZONE_SIDE_FRACTION
    return normalized / 100f
}

fun tapZoneSpans(
    direction: ReadingDirection,
    sideFraction: Float = DEFAULT_TAP_ZONE_SIDE_FRACTION,
): List<TapZoneSpan> {
    val side = sideFraction.coerceIn(0f, 0.5f)
    val left = if (direction == ReadingDirection.RTL) TapZone.Next else TapZone.Previous
    val right = if (direction == ReadingDirection.RTL) TapZone.Previous else TapZone.Next
    return listOf(
        TapZoneSpan(left, 0f, side),
        TapZoneSpan(TapZone.Menu, side, 1f - side),
        TapZoneSpan(right, 1f - side, 1f),
    )
}

fun tapZoneAt(
    x: Float,
    width: Float,
    direction: ReadingDirection,
    sideFraction: Float = DEFAULT_TAP_ZONE_SIDE_FRACTION,
): TapZone {
    val spans = tapZoneSpans(direction, sideFraction)
    if (width <= 0f) return TapZone.Menu
    val fraction = (x / width).coerceIn(0f, 1f)
    return when {
        fraction < spans[0].end -> spans[0].zone
        fraction < spans[1].end -> spans[1].zone
        else -> spans[2].zone
    }
}
