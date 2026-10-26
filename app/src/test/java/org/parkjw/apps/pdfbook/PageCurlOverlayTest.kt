package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun colorFilterTintsThePageBitmaps() {
        val pages = SolidPageBitmaps(1, 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        val page = runBlocking { pages.render(1, 360, 640)!! }
        composeRule.setContent {
            PageCurlOverlay(
                front = page,
                back = page,
                progress = { 0f },
                side = CurlSide.Right,
                modifier = Modifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(Color.Red),
            )
        }
        composeRule.waitForIdle()
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val pixel = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue("center pixel was $pixel", pixel.red > 0.8f && pixel.green < 0.2f)
    }
}
