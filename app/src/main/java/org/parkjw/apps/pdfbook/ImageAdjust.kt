package org.parkjw.apps.pdfbook

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeColorFilter
import org.parkjw.apps.pdfbook.settings.DisplayMode
import java.util.Locale
import kotlin.math.roundToInt

/** Per-book screen-only adjustment. Default values leave the page exactly as before. */
data class ImageAdjust(
    val brightness: Int = 0,
    val contrast: Int = 0,
    val gamma: Float = 1f,
    val grayscale: Int = 0,
) {
    val isDefault: Boolean
        get() = brightness == 0 && contrast == 0 && gamma == DEFAULT_GAMMA && grayscale == 0

    companion object {
        const val DEFAULT_GAMMA = 1f
        val Default = ImageAdjust()
        const val BRIGHTNESS_RANGE = 50
        const val CONTRAST_RANGE = 50
        const val GRAYSCALE_RANGE = 100
        const val GAMMA_MIN = 0.5f
        const val GAMMA_MAX = 2f
    }
}

/** Gamma needs a shader, so its row is offered only where RuntimeColorFilter exists. */
const val GAMMA_MIN_API = 36

internal fun gammaAvailable(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= GAMMA_MIN_API

internal fun encodeAdjust(adjust: ImageAdjust): String = listOf(
    adjust.brightness,
    adjust.contrast,
    String.format(Locale.ROOT, "%.2f", adjust.gamma),
    adjust.grayscale,
).joinToString(",")

internal fun decodeAdjust(raw: String?): ImageAdjust {
    val parts = raw?.split(",") ?: return ImageAdjust.Default
    if (parts.size != 4) return ImageAdjust.Default
    val brightness = parts[0].toIntOrNull() ?: return ImageAdjust.Default
    val contrast = parts[1].toIntOrNull() ?: return ImageAdjust.Default
    val gamma = parts[2].toFloatOrNull() ?: return ImageAdjust.Default
    val grayscale = parts[3].toIntOrNull() ?: return ImageAdjust.Default
    return ImageAdjust(brightness, contrast, gamma, grayscale).clamped()
}

internal fun ImageAdjust.clamped(): ImageAdjust = ImageAdjust(
    brightness = brightness.coerceIn(-ImageAdjust.BRIGHTNESS_RANGE, ImageAdjust.BRIGHTNESS_RANGE),
    contrast = contrast.coerceIn(-ImageAdjust.CONTRAST_RANGE, ImageAdjust.CONTRAST_RANGE),
    gamma = gamma.coerceIn(ImageAdjust.GAMMA_MIN, ImageAdjust.GAMMA_MAX),
    grayscale = grayscale.coerceIn(0, ImageAdjust.GRAYSCALE_RANGE),
)

private val IDENTITY_MATRIX = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/**
 * Brightness/contrast then grayscale as one matrix. Null when unchanged, so the
 * default path keeps the old pixels. Gamma is not expressible here; [readerColorFilter]
 * runs it in the API 36+ shader.
 */
internal fun ImageAdjust.matrixAdjustment(): FloatArray? {
    if (isDefault) return null
    val matrix = brightnessContrastMatrix(brightness, contrast)
    return if (grayscale == 0) matrix else concatColorMatrices(grayscaleMatrix(grayscale), matrix)
}

internal fun brightnessContrastMatrix(brightness: Int, contrast: Int): FloatArray {
    val scale = 1f + contrast / 100f
    val offset = -255f * contrast / 200f + 255f * brightness / 200f
    return floatArrayOf(
        scale, 0f, 0f, 0f, offset,
        0f, scale, 0f, 0f, offset,
        0f, 0f, scale, 0f, offset,
        0f, 0f, 0f, 1f, 0f,
    )
}

internal fun grayscaleMatrix(grayscale: Int): FloatArray {
    val g = grayscale.coerceIn(0, 100) / 100f
    val red = 1f - g + g * IMAGE_LUMA_RED
    val green = g * IMAGE_LUMA_GREEN
    val blue = g * IMAGE_LUMA_BLUE
    return floatArrayOf(
        red, green, blue, 0f, 0f,
        red, green, blue, 0f, 0f,
        red, green, blue, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** Applies [before] first and [after] second, matching the draw-time order. */
internal fun concatColorMatrices(after: FloatArray, before: FloatArray): FloatArray {
    val out = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..3) {
            var sum = 0f
            for (k in 0..3) sum += after[row * 5 + k] * before[k * 5 + col]
            out[row * 5 + col] = sum
        }
        var offset = after[row * 5 + 4]
        for (k in 0..3) offset += after[row * 5 + k] * before[k * 5 + 4]
        out[row * 5 + 4] = offset
    }
    return out
}

/** Adjustment first, then the page color, as the mockup's order line says. */
internal fun readerColorMatrix(displayMode: DisplayMode, adjust: ImageAdjust): FloatArray? {
    val adjusted = adjust.matrixAdjustment() ?: return displayMode.colorMatrix()
    val page = displayMode.colorMatrix() ?: return adjusted
    return concatColorMatrices(page, adjusted)
}

internal val ImageAdjust.needsGammaShader: Boolean
    get() = gamma != ImageAdjust.DEFAULT_GAMMA

/**
 * One filter for every draw path: the adjustment (then gamma where available)
 * followed by the page color. Null means "draw exactly as before".
 */
internal fun readerColorFilter(
    displayMode: DisplayMode,
    adjust: ImageAdjust,
    sdkInt: Int = Build.VERSION.SDK_INT,
): ColorFilter? {
    if (adjust.needsGammaShader && sdkInt >= GAMMA_MIN_API && Build.VERSION.SDK_INT >= GAMMA_MIN_API) {
        gammaColorFilter(adjust, displayMode.colorMatrix())?.let { return it }
    }
    return readerColorMatrix(displayMode, adjust)?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
}

/**
 * Non-linear gamma cannot be expressed by [ColorMatrix], so API 36+ runs
 * brightness/contrast, gamma, grayscale and the page-color matrix in one AGSL
 * pass. Null when the platform refuses the shader; callers fall back to the
 * matrix path without gamma.
 */
@RequiresApi(GAMMA_MIN_API)
internal fun gammaColorFilter(
    adjust: ImageAdjust,
    pageMatrix: FloatArray?,
): ColorFilter? = runCatching {
    val clamped = adjust.clamped()
    val filter = android.graphics.RuntimeColorFilter(GAMMA_SHADER)
    filter.setFloatUniform("bcScale", 1f + clamped.contrast / 100f)
    filter.setFloatUniform("bcOffset", (clamped.brightness - clamped.contrast) / 200f)
    filter.setFloatUniform("gamma", clamped.gamma)
    filter.setFloatUniform("gray", clamped.grayscale / 100f)
    val page = pageMatrix ?: IDENTITY_MATRIX
    filter.setFloatUniform("p0", page[0])
    filter.setFloatUniform("p1", page[1])
    filter.setFloatUniform("p2", page[2])
    filter.setFloatUniform("p3", page[4] / 255f)
    filter.setFloatUniform("p4", page[5])
    filter.setFloatUniform("p5", page[6])
    filter.setFloatUniform("p6", page[7])
    filter.setFloatUniform("p7", page[9] / 255f)
    filter.setFloatUniform("p8", page[10])
    filter.setFloatUniform("p9", page[11])
    filter.setFloatUniform("p10", page[12])
    filter.setFloatUniform("p11", page[14] / 255f)
    filter.asComposeColorFilter()
}.getOrNull()

/**
 * Color filter shader: brightness/contrast, then gamma, then grayscale, then the
 * page-color matrix. Channels are 0..1 and the offsets are pre-divided by 255.
 */
internal const val GAMMA_SHADER = """
    uniform float bcScale;
    uniform float bcOffset;
    uniform float gamma;
    uniform float gray;
    uniform float p0;
    uniform float p1;
    uniform float p2;
    uniform float p3;
    uniform float p4;
    uniform float p5;
    uniform float p6;
    uniform float p7;
    uniform float p8;
    uniform float p9;
    uniform float p10;
    uniform float p11;

    half4 main(half4 color) {
        float3 v = float3(color.rgb) * bcScale + bcOffset;
        v = pow(max(v, 0.0), 1.0 / max(gamma, 0.01));
        float luma = dot(v, float3(0.299, 0.587, 0.114));
        v = mix(v, float3(luma), gray);
        float3 outRgb = float3(
            dot(v, float3(p0, p1, p2)) + p3,
            dot(v, float3(p4, p5, p6)) + p7,
            dot(v, float3(p8, p9, p10)) + p11
        );
        return half4(clamp(outRgb, 0.0, 1.0), color.a);
    }
"""

/** Slider rounding: brightness/contrast step 5, gamma 0.05, grayscale 10. */
internal fun roundAdjust(adjust: ImageAdjust): ImageAdjust = ImageAdjust(
    brightness = (adjust.brightness / 5f).roundToInt() * 5,
    contrast = (adjust.contrast / 5f).roundToInt() * 5,
    gamma = (adjust.gamma * 20f).roundToInt() / 20f,
    grayscale = (adjust.grayscale / 10f).roundToInt() * 10,
).clamped()

internal const val IMAGE_LUMA_RED = 0.299f
internal const val IMAGE_LUMA_GREEN = 0.587f
internal const val IMAGE_LUMA_BLUE = 0.114f
