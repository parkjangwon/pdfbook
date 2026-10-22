package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class DisplayModeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun defaultHasNoFilter() {
        assertNull(DisplayMode.Default.colorMatrix())
        assertNull(DisplayMode.Default.colorFilter())
    }

    @Test
    fun invertMatrixFlipsRgbAndKeepsAlpha() {
        val m = DisplayMode.Invert.colorMatrix()!!
        assertEquals(20, m.size)
        assertEquals(-1f, m[0]); assertEquals(-1f, m[6]); assertEquals(-1f, m[12])
        assertEquals(255f, m[4]); assertEquals(255f, m[9]); assertEquals(255f, m[14])
        assertEquals(1f, m[18]); assertEquals(0f, m[19])
    }

    @Test
    fun sepiaMatrixRowsAreTheClassicWeightsAndKeepAlpha() {
        val m = DisplayMode.Sepia.colorMatrix()!!
        assertEquals(20, m.size)
        assertEquals(listOf(0.393f, 0.769f, 0.189f), m.slice(0..2))
        assertEquals(listOf(0.349f, 0.686f, 0.168f), m.slice(5..7))
        assertEquals(listOf(0.272f, 0.534f, 0.131f), m.slice(10..12))
        assertEquals(1f, m[18])
    }

    @Test
    fun displayModePersistsAndUnknownValueFallsBackToDefault() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val repo = SettingsRepository(store)
        assertEquals(DisplayMode.Default, AppSettings().displayMode)
        assertEquals(DisplayMode.Default, repo.settings.first().displayMode)
        repo.update { it.copy(displayMode = DisplayMode.Sepia) }
        assertEquals(DisplayMode.Sepia, repo.settings.first().displayMode)
        store.edit { it[SettingsRepository.DisplayModeKey] = "Neon" }
        assertEquals(DisplayMode.Default, repo.settings.first().displayMode)
    }

    @Test
    fun invertModeDrawsInvertedPage() {
        val (r, g, b) = renderedCorner(DisplayMode.Invert)
        // Page colour is white (255), so inversion must be near black.
        assertTrue("r=$r g=$g b=$b", r < 8 && g < 8 && b < 8)
    }

    @Test
    fun sepiaModeDrawsWarmTintedPage() {
        val (r, g, b) = renderedCorner(DisplayMode.Sepia)
        // White saturates r and g at 255, so only blue drops.
        assertTrue("r=$r g=$g b=$b", r >= g && g > b)
    }

    @Test
    fun defaultModeLeavesPageUntouched() {
        val (r, g, b) = renderedCorner(DisplayMode.Default)
        assertTrue("r=$r g=$g b=$b", r > 247 && g > 247 && b > 247)
    }

    private fun renderedCorner(mode: DisplayMode): Triple<Int, Int, Int> {
        composeRule.setContent {
            CompositionLocalProvider(LocalDisplayMode provides mode) {
                Box(Modifier.size(200.dp).testTag("page_host")) {
                    PageBitmap(
                        renderer = SolidPageBitmaps(1, 0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
                        page = 1,
                        maxWidth = 200,
                        maxHeight = 200,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val px = composeRule.onNodeWithTag("page_host").captureToImage().toPixelMap()
        // Corner pixel sits in the plain page background, away from the accent shapes.
        val c = px[2, px.height - 2]
        fun ch(v: Float) = (v * 255f + 0.5f).toInt()
        return Triple(ch(c.red), ch(c.green), ch(c.blue))
    }
}
