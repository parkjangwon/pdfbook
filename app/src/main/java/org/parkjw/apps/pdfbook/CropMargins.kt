package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val CROP_MAX_SIDE_FRACTION = 0.25f
const val CROP_MIN_CONTENT_FRACTION = 0.02f
const val CROP_BACKGROUND_TOLERANCE = 20
const val CROP_MIN_INK_FRACTION = 0.002f
private const val CROP_MIN_BACKGROUND_LUMA = 128f
private const val CROP_SCAN_MAX_PIXELS = 400_000

data class CropMargins(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    val isNone: Boolean get() = left == 0f && top == 0f && right == 0f && bottom == 0f
}

fun detectCropMargins(
    bitmap: Bitmap,
    tolerance: Int = CROP_BACKGROUND_TOLERANCE,
    paddingFraction: Float = 0.01f,
    maxSideFraction: Float = CROP_MAX_SIDE_FRACTION,
    minContentFraction: Float = CROP_MIN_CONTENT_FRACTION,
): CropMargins {
    if (bitmap.width <= 0 || bitmap.height <= 0) return CropMargins()
    val step = scanStep(bitmap)
    val background = estimateBackground(bitmap, step)
    // Very dark scans stay uncropped: this is a documented limit, not a detection target.
    if (background.luma < CROP_MIN_BACKGROUND_LUMA) return CropMargins()
    return scanCropMargins(
        bitmap = bitmap,
        background = background,
        tolerance = tolerance.coerceIn(0, 255),
        step = step,
        paddingFraction = paddingFraction,
        maxSideFraction = maxSideFraction,
        minContentFraction = minContentFraction,
    )
}

/**
 * Two-page rule: both pages share the vertical crop (the union of their content bounds) and one
 * outer side margin. The inner (spine) margin stays so the pages keep their alignment.
 *
 * The outer margin is the smaller of the two pages' outer margins, so neither page loses content,
 * and sharing it makes both halves the same size. The two-page curl compares the turning page
 * with the facing page of the next spread within 1px, so unequal halves made every turn Slide.
 */
fun alignedTwoPageMargins(
    leftPage: CropMargins,
    rightPage: CropMargins,
): Pair<CropMargins, CropMargins> {
    val top = min(leftPage.top, rightPage.top)
    val bottom = min(leftPage.bottom, rightPage.bottom)
    val outer = min(leftPage.left, rightPage.right)
    return CropMargins(left = outer, top = top, right = 0f, bottom = bottom) to
        CropMargins(left = 0f, top = top, right = outer, bottom = bottom)
}

private data class Background(val red: Int, val green: Int, val blue: Int) {
    val luma: Float get() = 0.299f * red + 0.587f * green + 0.114f * blue
}

private fun scanCropMargins(
    bitmap: Bitmap,
    background: Background,
    tolerance: Int,
    step: Int,
    paddingFraction: Float,
    maxSideFraction: Float,
    minContentFraction: Float,
): CropMargins {
    val width = bitmap.width
    val height = bitmap.height
    val sampledWidth = (width + step - 1) / step
    val sampledHeight = (height + step - 1) / step
    val rowInk = IntArray(sampledHeight)
    val columnInk = IntArray(sampledWidth)
    val row = IntArray(width)
    for (sampleY in 0 until sampledHeight) {
        val y = sampleY * step
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (sampleX in 0 until sampledWidth) {
            val pixel = row[sampleX * step]
            if (isBackground(pixel, background, tolerance)) continue
            rowInk[sampleY]++
            columnInk[sampleX]++
        }
    }
    val minRowInk = max(2, ceil(sampledWidth * CROP_MIN_INK_FRACTION).toInt())
    val minColumnInk = max(2, ceil(sampledHeight * CROP_MIN_INK_FRACTION).toInt())
    var firstRow = -1
    var lastRow = -1
    for (y in 0 until sampledHeight) {
        if (rowInk[y] < minRowInk) continue
        if (firstRow < 0) firstRow = y
        lastRow = y
    }
    var firstColumn = -1
    var lastColumn = -1
    for (x in 0 until sampledWidth) {
        if (columnInk[x] < minColumnInk) continue
        if (firstColumn < 0) firstColumn = x
        lastColumn = x
    }
    if (firstRow < 0 || firstColumn < 0) return CropMargins()
    // Convert sampled coordinates back to source pixels (a sampled cell covers `step` pixels).
    val contentLeft = firstColumn * step
    val contentTop = firstRow * step
    val contentRight = min(width, (lastColumn + 1) * step) - 1
    val contentBottom = min(height, (lastRow + 1) * step) - 1
    val contentWidth = contentRight - contentLeft + 1
    val contentHeight = contentBottom - contentTop + 1
    if (contentWidth.toFloat() * contentHeight < minContentFraction * width * height) {
        return CropMargins()
    }
    val padX = (width * paddingFraction).roundToInt()
    val padY = (height * paddingFraction).roundToInt()
    return CropMargins(
        left = sideFraction(contentLeft - padX, width, maxSideFraction),
        top = sideFraction(contentTop - padY, height, maxSideFraction),
        right = sideFraction(width - 1 - contentRight - padX, width, maxSideFraction),
        bottom = sideFraction(height - 1 - contentBottom - padY, height, maxSideFraction),
    )
}

private fun isBackground(pixel: Int, background: Background, tolerance: Int): Boolean {
    if ((pixel ushr 24) and 0xFF < 16) return true
    val red = (pixel ushr 16) and 0xFF
    val green = (pixel ushr 8) and 0xFF
    val blue = pixel and 0xFF
    return abs(red - background.red) <= tolerance &&
        abs(green - background.green) <= tolerance &&
        abs(blue - background.blue) <= tolerance
}

/**
 * Estimates the paper color from the border pixels (median per channel), so cream and sepia
 * scans are measured against their own paper instead of an assumed pure white.
 */
private fun estimateBackground(bitmap: Bitmap, step: Int): Background {
    val width = bitmap.width
    val height = bitmap.height
    val reds = ArrayList<Int>()
    val greens = ArrayList<Int>()
    val blues = ArrayList<Int>()
    val row = IntArray(width)
    for (y in intArrayOf(0, height - 1)) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        var x = 0
        while (x < width) {
            addSample(row[x], reds, greens, blues)
            x += step
        }
    }
    var y = 0
    while (y < height) {
        addSample(bitmap.getPixel(0, y), reds, greens, blues)
        addSample(bitmap.getPixel(width - 1, y), reds, greens, blues)
        y += step
    }
    if (reds.isEmpty()) return Background(255, 255, 255)
    return Background(median(reds), median(greens), median(blues))
}

private fun addSample(pixel: Int, reds: MutableList<Int>, greens: MutableList<Int>, blues: MutableList<Int>) {
    if ((pixel ushr 24) and 0xFF < 16) return
    reds.add((pixel ushr 16) and 0xFF)
    greens.add((pixel ushr 8) and 0xFF)
    blues.add(pixel and 0xFF)
}

private fun median(values: MutableList<Int>): Int {
    values.sort()
    return values[values.size / 2]
}

private fun sideFraction(marginPx: Int, size: Int, maxSideFraction: Float): Float =
    (marginPx.coerceAtLeast(0).toFloat() / size).coerceIn(0f, maxSideFraction)

private fun scanStep(bitmap: Bitmap): Int {
    val pixels = bitmap.width.toLong() * bitmap.height
    if (pixels <= CROP_SCAN_MAX_PIXELS) return 1
    return ceil(sqrt(pixels.toDouble() / CROP_SCAN_MAX_PIXELS)).toInt().coerceAtLeast(1)
}
