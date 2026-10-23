package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val CROP_MAX_SIDE_FRACTION = 0.25f
const val CROP_MIN_CONTENT_FRACTION = 0.02f
const val CROP_BACKGROUND_TOLERANCE = 16
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
    val scan = downscaledForScan(bitmap)
    return try {
        scanCropMargins(scan, tolerance, paddingFraction, maxSideFraction, minContentFraction)
    } finally {
        if (scan !== bitmap) scan.recycle()
    }
}

/**
 * Two-page rule (conservative, pending a product decision): both pages share the vertical crop
 * (the union of their content bounds) and only the outer side margin is cropped. The inner
 * (spine) margin stays so the pages keep their alignment.
 */
fun alignedTwoPageMargins(
    leftPage: CropMargins,
    rightPage: CropMargins,
): Pair<CropMargins, CropMargins> {
    val top = min(leftPage.top, rightPage.top)
    val bottom = min(leftPage.bottom, rightPage.bottom)
    return CropMargins(left = leftPage.left, top = top, right = 0f, bottom = bottom) to
        CropMargins(left = 0f, top = top, right = rightPage.right, bottom = bottom)
}

private fun scanCropMargins(
    bitmap: Bitmap,
    tolerance: Int,
    paddingFraction: Float,
    maxSideFraction: Float,
    minContentFraction: Float,
): CropMargins {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val threshold = 255 - tolerance.coerceIn(0, 255)
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            val pixel = pixels[row + x]
            if ((pixel ushr 24) and 0xFF < 16) continue
            val red = (pixel ushr 16) and 0xFF
            val green = (pixel ushr 8) and 0xFF
            val blue = pixel and 0xFF
            if (red >= threshold && green >= threshold && blue >= threshold) continue
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
    }
    if (maxX < minX || maxY < minY) return CropMargins()
    val contentWidth = maxX - minX + 1
    val contentHeight = maxY - minY + 1
    if (contentWidth.toFloat() * contentHeight < minContentFraction * width * height) {
        return CropMargins()
    }
    val padX = (width * paddingFraction).roundToInt()
    val padY = (height * paddingFraction).roundToInt()
    return CropMargins(
        left = sideFraction(minX - padX, width, maxSideFraction),
        top = sideFraction(minY - padY, height, maxSideFraction),
        right = sideFraction(width - 1 - maxX - padX, width, maxSideFraction),
        bottom = sideFraction(height - 1 - maxY - padY, height, maxSideFraction),
    )
}

private fun sideFraction(marginPx: Int, size: Int, maxSideFraction: Float): Float =
    (marginPx.coerceAtLeast(0).toFloat() / size).coerceIn(0f, maxSideFraction)

private fun downscaledForScan(bitmap: Bitmap): Bitmap {
    val pixels = bitmap.width.toLong() * bitmap.height
    if (pixels <= CROP_SCAN_MAX_PIXELS) return bitmap
    val scale = sqrt(CROP_SCAN_MAX_PIXELS.toFloat() / pixels)
    val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}
