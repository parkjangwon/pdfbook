package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
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
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.ui.theme.ReaderListCurrent
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBack
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBackSepia
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadow
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadowSepia
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
    fun sepiaMatrixMapsWhiteToPaperAndKeepsGreyReadable() {
        val m = DisplayMode.Sepia.colorMatrix()!!
        assertEquals(20, m.size)
        val paper = applyMatrix(m, 255, 255, 255)
        assertEquals(0xF4, paper[0])
        assertEquals(0xEC, paper[1])
        assertEquals(0xD8, paper[2])
        val grey = applyMatrix(m, 0x80, 0x80, 0x80)
        assertEquals(0x72, grey[0])
        assertEquals(0x69, grey[1])
        assertEquals(0x5B, grey[2])
        val ratio = contrastRatio(paper, grey)
        assertTrue("ratio=$ratio", ratio >= 4.5)
        assertEquals(1f, m[18])
    }

    @Test
    fun curlLookCarriesThePerModePaperAndShadow() {
        val normal = DisplayMode.Default.curlLook()
        assertEquals(ReaderPageBack, normal.flapColor)
        assertEquals(ReaderPageShadow, normal.shadowColor)
        assertEquals(0.24f, normal.shadowAlpha, 0f)
        assertEquals(0.10f, normal.shadowMidAlpha, 0f)

        val sepia = DisplayMode.Sepia.curlLook()
        assertEquals(ReaderPageBackSepia, sepia.flapColor)
        assertEquals(ReaderPageShadowSepia, sepia.shadowColor)
        assertEquals(0.26f, sepia.shadowAlpha, 0f)
        assertEquals(0.11f, sepia.shadowMidAlpha, 0f)

        val inverted = DisplayMode.Invert.curlLook()
        assertEquals(ReaderListCurrent, inverted.flapColor)
        assertEquals(ReaderPageShadow, inverted.shadowColor)
        assertEquals(0.50f, inverted.shadowAlpha, 0f)
        assertEquals(0.20f, inverted.shadowMidAlpha, 0f)
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
    fun sepiaModeMapsMidGreyThroughTheMatrix() {
        val (r, g, b) = renderedCorner(DisplayMode.Sepia, page = 0xFF808080.toInt())
        // #808080 through the paper/ink matrix lands on #72695B (4.59:1 on the paper).
        assertTrue("r=$r g=$g b=$b", abs(r - 0x72) <= 4 && abs(g - 0x69) <= 4 && abs(b - 0x5B) <= 4)
    }

    @Test
    fun readerScreenPassesDisplayModeToPages() {
        val spreads = spreads(1, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(1, 0xFFFFFFFF.toInt(), 0xFF808080.toInt()),
                    pagerState = rememberPagerState(pageCount = { spreads.size }),
                    spreads = spreads,
                    title = "t",
                    pageCount = 1,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    showTapZoneGuide = false,
                    displayMode = DisplayMode.Invert,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        val px = composeRule.onRoot().captureToImage().toPixelMap()
        var dark = 0
        for (y in 0 until px.height) for (x in 0 until px.width) {
            val c = px[x, y]
            if (c.red < 0.05f && c.green < 0.05f && c.blue < 0.05f) dark++
        }
        // A white page under Light theme is the only black-capable area once inverted.
        assertTrue("dark=$dark", dark > 1000)
    }

    @Test
    fun defaultModeLeavesPageUntouched() {
        val (r, g, b) = renderedCorner(DisplayMode.Default)
        assertTrue("r=$r g=$g b=$b", r > 247 && g > 247 && b > 247)
    }

    private fun renderedCorner(mode: DisplayMode, page: Int = 0xFFFFFFFF.toInt()): Triple<Int, Int, Int> {
        composeRule.setContent {
            CompositionLocalProvider(LocalDisplayMode provides mode) {
                Box(Modifier.size(200.dp).testTag("page_host")) {
                    PageBitmap(
                        renderer = SolidPageBitmaps(1, page, 0xFF000000.toInt()),
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

    private fun applyMatrix(m: FloatArray, r: Int, g: Int, b: Int): IntArray = IntArray(3) { c ->
        (m[c * 5] * r + m[c * 5 + 1] * g + m[c * 5 + 2] * b + m[c * 5 + 4])
            .roundToInt()
            .coerceIn(0, 255)
    }

    private fun contrastRatio(a: IntArray, b: IntArray): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun relativeLuminance(rgb: IntArray): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(rgb[0]) + 0.7152 * channel(rgb[1]) + 0.0722 * channel(rgb[2])
    }
}
