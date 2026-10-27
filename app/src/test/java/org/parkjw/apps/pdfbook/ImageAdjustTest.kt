package org.parkjw.apps.pdfbook

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.settings.DisplayMode

class ImageAdjustTest {
    private fun apply(matrix: FloatArray, color: FloatArray): FloatArray {
        val out = FloatArray(3)
        for (row in 0..2) {
            out[row] = matrix[row * 5] * color[0] +
                matrix[row * 5 + 1] * color[1] +
                matrix[row * 5 + 2] * color[2] +
                matrix[row * 5 + 4]
        }
        return out
    }

    @Test
    fun defaultHasNoMatrixAndNoFilter() {
        val adjust = ImageAdjust.Default
        assertTrue(adjust.isDefault)
        assertNull(adjust.matrixAdjustment())
        assertNull(readerColorMatrix(DisplayMode.Default, adjust))
        assertNull(readerColorFilter(DisplayMode.Default, adjust, sdkInt = 35))
        assertNull(readerColorFilter(DisplayMode.Default, adjust, sdkInt = 36))
    }

    @Test
    fun brightnessMovesEveryChannelByHalfPercent() {
        val matrix = brightnessContrastMatrix(brightness = 50, contrast = 0)
        val out = apply(matrix, floatArrayOf(128f, 128f, 128f))
        assertEquals(128f + 63.75f, out[0], 1e-4f)
        assertEquals(128f + 63.75f, out[1], 1e-4f)
        assertEquals(128f + 63.75f, out[2], 1e-4f)
    }

    @Test
    fun contrastPivotsAroundHalf() {
        val matrix = brightnessContrastMatrix(brightness = 0, contrast = 50)
        val mid = apply(matrix, floatArrayOf(127.5f, 127.5f, 127.5f))
        assertEquals(127.5f, mid[0], 1e-4f)
        val dark = apply(matrix, floatArrayOf(64f, 64f, 64f))
        assertEquals(32.25f, dark[0], 1e-4f)
        val light = apply(matrix, floatArrayOf(192f, 192f, 192f))
        assertEquals(224.25f, light[0], 1e-4f)
    }

    @Test
    fun grayscaleUsesRec709LumaAtFullStrength() {
        val matrix = grayscaleMatrix(100)
        val red = apply(matrix, floatArrayOf(255f, 0f, 0f))
        assertEquals(76.245f, red[0], 1e-3f)
        assertEquals(76.245f, red[1], 1e-3f)
        assertEquals(76.245f, red[2], 1e-3f)
    }

    @Test
    fun grayscaleIsAPartialMix() {
        val matrix = grayscaleMatrix(50)
        val out = apply(matrix, floatArrayOf(255f, 0f, 0f))
        assertEquals(255f * 0.5f + 76.245f * 0.5f, out[0], 1e-3f)
    }

    @Test
    fun adjustmentRunsBeforePageColor() {
        val adjust = ImageAdjust(brightness = 50)
        val matrix = readerColorMatrix(DisplayMode.Invert, adjust)!!
        val out = apply(matrix, floatArrayOf(51f, 51f, 51f))
        // Brightness first (51 + 63.75), then the page inverts it.
        assertEquals(255f - 114.75f, out[0], 1e-4f)
        assertEquals(255f - 114.75f, out[1], 1e-4f)
        assertEquals(255f - 114.75f, out[2], 1e-4f)
    }

    @Test
    fun sepiaRunsAfterAdjustment() {
        val adjust = ImageAdjust(brightness = 25)
        val matrix = readerColorMatrix(DisplayMode.Sepia, adjust)!!
        val out = apply(matrix, floatArrayOf(128f, 128f, 128f))
        val expected = apply(DisplayMode.Sepia.colorMatrix()!!, apply(brightnessContrastMatrix(25, 0), floatArrayOf(128f, 128f, 128f)))
        assertArrayEquals(expected, out, 1e-3f)
    }

    @Test
    fun pageColorAloneKeepsTheOldFilterPath() {
        val matrix = readerColorMatrix(DisplayMode.Invert, ImageAdjust.Default)!!
        assertArrayEquals(DisplayMode.Invert.colorMatrix()!!, matrix, 1e-6f)
    }

    @Test
    fun gammaNeedsApi36() {
        assertFalse(gammaAvailable(35))
        assertTrue(gammaAvailable(36))
        val adjust = ImageAdjust(gamma = 2f)
        // Below API 36 the gamma value cannot run, so the matrix path ignores it.
        val matrix = readerColorMatrix(DisplayMode.Default, adjust)!!
        assertArrayEquals(brightnessContrastMatrix(0, 0), matrix, 1e-6f)
        assertNull(readerColorFilter(DisplayMode.Default, ImageAdjust.Default, sdkInt = 35))
    }

    @Test
    fun roundAdjustSnapsToSteps() {
        assertEquals(ImageAdjust(brightness = 10, contrast = -15, gamma = 1.1f, grayscale = 30), roundAdjust(ImageAdjust(12, -14, 1.12f, 31)))
        assertEquals(ImageAdjust(gamma = 0.5f), roundAdjust(ImageAdjust(gamma = 0.1f)))
        assertEquals(ImageAdjust(gamma = 2f), roundAdjust(ImageAdjust(gamma = 3f)))
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val adjust = ImageAdjust(brightness = -15, contrast = 25, gamma = 1.35f, grayscale = 70)
        assertEquals(adjust, decodeAdjust(encodeAdjust(adjust)))
    }

    @Test
    fun decodeClampsAndFallsBackToDefault() {
        assertEquals(ImageAdjust(50, -50, 2f, 100), decodeAdjust("99,-99,9.9,300"))
        assertEquals(ImageAdjust.Default, decodeAdjust(null))
        assertEquals(ImageAdjust.Default, decodeAdjust("garbage"))
        assertEquals(ImageAdjust.Default, decodeAdjust("1,2,3"))
        assertEquals(ImageAdjust.Default, decodeAdjust("1,2,x,4"))
    }

    @Test
    fun storedGammaValuesCompareAsDefaultOnlyWhenExactlyOne() {
        assertFalse(ImageAdjust(gamma = 1.05f).isDefault)
        assertTrue(ImageAdjust(gamma = "1.00".toFloat()).isDefault)
    }

}
