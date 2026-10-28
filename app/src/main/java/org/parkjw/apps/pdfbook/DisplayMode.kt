package org.parkjw.apps.pdfbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderListCurrent
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBack
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBackSepia
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageFoldHighlight
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadow
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadowSepia
import org.parkjw.apps.pdfbook.ui.theme.ReaderSepiaContrast
import org.parkjw.apps.pdfbook.ui.theme.ReaderSepiaInk
import org.parkjw.apps.pdfbook.ui.theme.ReaderSepiaPaper

/** Applied at draw time, so cached page bitmaps stay valid when the mode changes. */
val LocalDisplayMode = compositionLocalOf { DisplayMode.Default }

/** The last read book's screen-only adjustment; set by the reader route. */
val LocalImageAdjust = compositionLocalOf { ImageAdjust.Default }

private const val LUMA_RED = 0.299f
private const val LUMA_GREEN = 0.587f
private const val LUMA_BLUE = 0.114f

internal fun DisplayMode.colorMatrix(): FloatArray? = when (this) {
    DisplayMode.Default -> null
    DisplayMode.Invert -> floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )
    DisplayMode.Sepia -> sepiaMatrix()
}

/**
 * Sepia interpolates brightness between [paper] and [ink] and widens contrast by
 * [contrast], so white lands on the paper colour and dark text stays darker than
 * [ink]. Rows and offsets are computed from the tokens (DESIGN.md rule 4).
 */
internal fun sepiaMatrix(
    paper: Color = ReaderSepiaPaper,
    ink: Color = ReaderSepiaInk,
    contrast: Float = ReaderSepiaContrast,
): FloatArray {
    val paperRgb = floatArrayOf(paper.red * 255f, paper.green * 255f, paper.blue * 255f)
    val inkRgb = floatArrayOf(ink.red * 255f, ink.green * 255f, ink.blue * 255f)
    val luma = floatArrayOf(LUMA_RED, LUMA_GREEN, LUMA_BLUE)
    val matrix = FloatArray(20)
    for (channel in 0..2) {
        val span = (paperRgb[channel] - inkRgb[channel]) * contrast / 255f
        matrix[channel * 5] = span * luma[0]
        matrix[channel * 5 + 1] = span * luma[1]
        matrix[channel * 5 + 2] = span * luma[2]
        matrix[channel * 5 + 4] =
            inkRgb[channel] + (paperRgb[channel] - inkRgb[channel]) * (1f - contrast)
    }
    matrix[18] = 1f
    return matrix
}

internal fun DisplayMode.colorFilter(): ColorFilter? =
    colorMatrix()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }

/** Adjustment then page color, built once per value change, never per frame. */
@Composable
internal fun rememberReaderColorFilter(displayMode: DisplayMode): ColorFilter? {
    val adjust = LocalImageAdjust.current
    return remember(displayMode, adjust) { readerColorFilter(displayMode, adjust) }
}

/** Per-mode paper, shadow and fold values for the page-curl overlay (v0.7.0 mockups). */
internal data class CurlLook(
    val flapColor: Color,
    val shadowColor: Color,
    val shadowAlpha: Float,
    val shadowMidAlpha: Float,
    val foldHighlightColor: Color,
)

internal fun DisplayMode.curlLook(): CurlLook = when (this) {
    DisplayMode.Default -> CurlLook(
        flapColor = ReaderPageBack,
        shadowColor = ReaderPageShadow,
        shadowAlpha = 0.24f,
        shadowMidAlpha = 0.10f,
        foldHighlightColor = ReaderPageFoldHighlight.copy(alpha = 0.50f),
    )
    DisplayMode.Sepia -> CurlLook(
        flapColor = ReaderPageBackSepia,
        shadowColor = ReaderPageShadowSepia,
        shadowAlpha = 0.26f,
        shadowMidAlpha = 0.11f,
        foldHighlightColor = ReaderPageFoldHighlight.copy(alpha = 0.45f),
    )
    DisplayMode.Invert -> CurlLook(
        flapColor = ReaderListCurrent,
        shadowColor = ReaderPageShadow,
        shadowAlpha = 0.50f,
        shadowMidAlpha = 0.20f,
        foldHighlightColor = ReaderPageFoldHighlight.copy(alpha = 0.12f),
    )
}
