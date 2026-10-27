package org.parkjw.apps.pdfbook

import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
