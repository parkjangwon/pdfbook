package org.parkjw.apps.pdfbook

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric's software canvas does not render RuntimeColorFilter yet (a native
 * ColorMatrix filter does change pixels, a runtime one does not), so these tests
 * cover construction and the shader contract. The gamma pixels need a real
 * Android 16 device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ImageAdjustGammaTest {
    private val uniforms = listOf(
        "bcScale", "bcOffset", "gamma", "gray",
        "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11",
    )

    @Test
    fun runtimeFilterBuildsOnApi36() {
        assertNotNull(gammaColorFilter(ImageAdjust(gamma = 2f), pageMatrix = null))
        assertNotNull(gammaColorFilter(ImageAdjust(gamma = 0.5f, brightness = 20, grayscale = 40), DisplayMode.Sepia.colorMatrix()))
        assertNotNull(gammaColorFilter(ImageAdjust(gamma = 1.5f), DisplayMode.Invert.colorMatrix()))
    }

    @Test
    fun everyUniformTheCodeSetsIsDeclaredInTheShader() {
        for (name in uniforms) {
            assertTrue("missing uniform $name", Regex("""uniform\s+float\s+$name\s*;""").containsMatchIn(GAMMA_SHADER))
        }
        assertTrue(GAMMA_SHADER.contains("pow("))
        assertTrue(GAMMA_SHADER.contains("mix("))
    }

    /**
     * With gamma 1 the shader must reproduce the matrix path exactly, including
     * values that overshoot below 0 before the page color: no clamp between
     * stages, only the final one.
     */
    @Test
    fun theShaderClampsOnlyAtTheEndLikeTheMatrixPath() {
        val adjustments = listOf(
            ImageAdjust(brightness = 40),
            ImageAdjust(brightness = -40),
            ImageAdjust(contrast = 50),
            ImageAdjust(contrast = -50),
            ImageAdjust(brightness = 25, contrast = 25, grayscale = 50),
            ImageAdjust(brightness = -30, contrast = 40, grayscale = 100),
        )
        val steps = listOf(0f, 0.05f, 0.25f, 0.5f, 0.95f, 1f)
        for (mode in listOf(DisplayMode.Default, DisplayMode.Invert, DisplayMode.Sepia)) {
            for (adjust in adjustments) {
                val matrix = readerColorMatrix(mode, adjust)!!
                val page = mode.colorMatrix()
                for (r in steps) for (g in steps) for (b in steps) {
                    val shader = shaderRgb(floatArrayOf(r, g, b), adjust, page)
                    for (channel in 0..2) {
                        val expected = (
                            matrix[channel * 5] * r * 255f +
                                matrix[channel * 5 + 1] * g * 255f +
                                matrix[channel * 5 + 2] * b * 255f +
                                matrix[channel * 5 + 4]
                            ).coerceIn(0f, 255f) / 255f
                        assertEquals(
                            "$mode $adjust rgb($r,$g,$b) channel $channel",
                            expected,
                            shader[channel],
                            2f / 255f,
                        )
                    }
                }
            }
        }
    }

    /** Kotlin transcription of [GAMMA_SHADER] with gamma 1. */
    private fun shaderRgb(rgb: FloatArray, adjust: ImageAdjust, page: FloatArray?): FloatArray {
        val scale = 1f + adjust.contrast / 100f
        val offset = (adjust.brightness - adjust.contrast) / 200f
        val v = FloatArray(3) { i ->
            val linear = rgb[i] * scale + offset
            if (linear < 0f) linear else Math.pow(maxOf(linear, 0f).toDouble(), 1.0).toFloat()
        }
        val luma = v[0] * 0.299f + v[1] * 0.587f + v[2] * 0.114f
        val gray = adjust.grayscale / 100f
        val mixed = FloatArray(3) { i -> v[i] + (luma - v[i]) * gray }
        val p = page ?: floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        return FloatArray(3) { row ->
            (p[row * 5] * mixed[0] + p[row * 5 + 1] * mixed[1] + p[row * 5 + 2] * mixed[2] + p[row * 5 + 4] / 255f)
                .coerceIn(0f, 1f)
        }
    }
}
