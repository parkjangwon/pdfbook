package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.PageBitmaps
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.spreadIndexOf
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SpreadShiftScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun compactOff() = capture("reader-spread-shift-menu-360-off.png", shift = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun compactOn() = capture("reader-spread-shift-menu-360-on.png", shift = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun compactOnInverted() =
        capture("reader-spread-shift-menu-360-on-inverted.png", shift = true, displayMode = DisplayMode.Invert)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun compactOffSepia() =
        capture("reader-spread-shift-menu-360-off-sepia.png", shift = false, displayMode = DisplayMode.Sepia)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun singlePage() =
        capture("reader-spread-shift-menu-single-360.png", shift = false, twoPage = false)

    @Test
    @Config(qualifiers = "ko-w320dp-h920dp-xxhdpi")
    fun compact320Font2() =
        capture("reader-spread-shift-menu-320-font2.png", shift = true, fontScale = 2f)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun expandedOn() = capture("reader-spread-shift-menu-expanded.png", shift = true)

    private fun capture(
        file: String,
        shift: Boolean,
        twoPage: Boolean = true,
        displayMode: DisplayMode = DisplayMode.Default,
        fontScale: Float = 1f,
    ) {
        val pageCount = 30
        val pageSpreads = spreads(
            pageCount,
            twoPage = twoPage,
            coverAlone = true,
            shift = if (shift) 1 else 0,
        )
        val index = spreadIndexOf(pageSpreads, 12)
        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = base.density, fontScale = fontScale),
                ) {
                    ReaderScreen(
                        renderer = NumberedPageBitmaps(pageCount),
                        pagerState = rememberPagerState(initialPage = index, pageCount = { pageSpreads.size }),
                        spreads = pageSpreads,
                        title = "바닷가 마을 이야기",
                        pageCount = pageCount,
                        currentPage = 12,
                        twoPage = twoPage,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.RTL,
                        overlayVisible = true,
                        showTapZoneGuide = false,
                        displayMode = displayMode,
                        spreadShift = shift,
                        onSpreadShiftChange = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        captureScreenRoboImage(file)
    }

    private class NumberedPageBitmaps(override val pageCount: Int) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
            val width = maxWidth.coerceAtMost(480).coerceAtLeast(1)
            val height = maxHeight.coerceAtMost(720).coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(0xFFF7F2E8.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF2B2B2B.toInt()
                textSize = height / 4f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(page.toString(), width / 2f, height / 2f, paint)
            return bitmap
        }
    }
}
