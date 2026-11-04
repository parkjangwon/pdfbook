package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel checks for a wide page shown alone in Book posture (V071-TWOPAGE.md §4, option A): the page
 * is laid out on the virtual spread and shifted by the hinge gap, so it reaches both window edges
 * and only the hinge itself stays background. Layout bounds alone pass the old offset bug.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w841dp-h701dp-xxhdpi")
class WidePageAloneBookPixelsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pageCount = 40
    private val paper = 0xFFF7F2E8.toInt()
    private val background = 0xFF000000.toInt()
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun widePageReachesBothWindowEdges() {
        showWidePageOnBook()
        val px = capture()
        val row = px.height / 2
        assertEquals("left edge starts the page", red, px[0, row].toArgb())
        assertEquals("right edge still shows the page", blue, px[px.width - 1, row].toArgb())
    }

    @Test
    fun onlyTheHingeGapStaysBackground() {
        showWidePageOnBook()
        val px = capture()
        val row = px.height / 2
        val gapLeft = px.width / 2 - 8
        val gapRight = px.width / 2 + 8
        assertEquals("page before the hinge", paper, px[gapLeft - 2, row].toArgb())
        assertEquals("inside the hinge", background, px[gapLeft + 2, row].toArgb())
        assertEquals("page resumes after the hinge", paper, px[gapRight + 2, row].toArgb())
        assertEquals("background only in the hinge", paper, px[gapRight + 6, row].toArgb())
    }

    private companion object {
        const val PAPER = 0xFFF7F2E8.toInt()
        const val RED = 0xFFFF0000.toInt()
        const val BLUE = 0xFF0000FF.toInt()
    }

    @Test
    fun turningOffTheWidePageSlidesInsteadOfCurlingOverTheHinge() {
        showWidePageOnBook()
        composeRule.onNodeWithTag("reader_screen").performTouchInput {
            click(Offset(width * 0.85f, height / 2f))
        }
        composeRule.waitForIdle()
        // A curl over the hinge would compose the overlay at some progress; the hinge guard slides.
        composeRule.onAllNodesWithTag("reader_page_curl").assertCountEquals(0)
        composeRule.onNodeWithTag("reader_page_13").assertIsDisplayed()
    }

    private fun capture() = composeRule.onNodeWithTag("reader_screen").captureToImage().toPixelMap()

    /** A page wider than the virtual spread, with a 1px marker on each edge of the virtual spread. */
    private class WidePageBitmaps(override val pageCount: Int) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): android.graphics.Bitmap {
            val width = maxWidth.coerceAtLeast(2)
            val height = (width * 3 / 5).coerceAtLeast(1)
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(PAPER)
            for (y in 0 until height) {
                bitmap.setPixel(0, y, RED)
                bitmap.setPixel(width - 1, y, BLUE)
            }
            return bitmap
        }
    }

    private fun showWidePageOnBook() {
        val wide = setOf(12)
        val pageSpreads = spreads(pageCount, twoPage = true, coverAlone = true, widePages = wide)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val mid = constraints.maxWidth / 2
                    val posture = FoldPosture.Book(IntRect(mid - 8, 0, mid + 8, constraints.maxHeight))
                    val pagerState = rememberPagerState(
                        initialPage = spreadIndexOf(pageSpreads, 12),
                        pageCount = { pageSpreads.size },
                    )
                    ReaderScreen(
                        renderer = WidePageBitmaps(pageCount),
                        pagerState = pagerState,
                        spreads = pageSpreads,
                        title = "책",
                        pageCount = pageCount,
                        currentPage = 12,
                        twoPage = true,
                        posture = posture,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        showTapZoneGuide = false,
                        widePages = wide,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
