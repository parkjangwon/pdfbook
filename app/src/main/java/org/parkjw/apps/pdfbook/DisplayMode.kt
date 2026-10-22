package org.parkjw.apps.pdfbook

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import org.parkjw.apps.pdfbook.settings.DisplayMode

/** Applied at draw time, so cached page bitmaps stay valid when the mode changes. */
val LocalDisplayMode = compositionLocalOf { DisplayMode.Default }

internal fun DisplayMode.colorMatrix(): FloatArray? = when (this) {
    DisplayMode.Default -> null
    DisplayMode.Invert -> floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )
    DisplayMode.Sepia -> floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

internal fun DisplayMode.colorFilter(): ColorFilter? =
    colorMatrix()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
