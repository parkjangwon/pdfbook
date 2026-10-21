package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.FoldPosture
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreadIndexOf
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class FoldPostureScreenshotsTest {
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
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun zf1TabletopMenuClosed() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.LTR,
            overlay = false,
            posture = ::tabletop,
        )
        capture("reader-fold-tabletop-closed-light.png")
    }

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun zf1TabletopMenuOpen() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.LTR,
            overlay = true,
            posture = ::tabletop,
        )
        capture("reader-fold-tabletop-open-light.png")
    }

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun zf2TabletopRtl() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.RTL,
            overlay = false,
            posture = ::tabletop,
        )
        capture("reader-fold-tabletop-rtl-light.png")
    }

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun zf4BookAutoMenuOpen() {
        setReader(
            page = 12,
            twoPage = true,
            direction = ReadingDirection.LTR,
            overlay = true,
            posture = ::book,
        )
        capture("reader-fold-book-auto-open-light.png")
    }

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun zf5BookAlwaysSingle() {
        setReader(
            page = 12,
            twoPage = false,
            direction = ReadingDirection.LTR,
            overlay = false,
            posture = ::book,
        )
        capture("reader-fold-book-single-light.png")
    }

    private fun tabletop(width: Int, height: Int): FoldPosture {
        val mid = height / 2
        return FoldPosture.Tabletop(IntRect(0, mid - 8, width, mid + 8))
    }

    private fun book(width: Int, height: Int): FoldPosture {
        val mid = width / 2
        return FoldPosture.Book(IntRect(mid - 8, 0, mid + 8, height))
    }

    private fun setReader(
        page: Int,
        twoPage: Boolean,
        direction: ReadingDirection,
        overlay: Boolean,
        posture: (Int, Int) -> FoldPosture,
    ) {
        val pageCount = 40
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        val index = spreadIndexOf(pageSpreads, page)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    ReaderScreen(
                        renderer = SolidPageBitmaps(
                            pageCount = pageCount,
                            pageColor = 0xFFF7F2E8.toInt(),
                            accentColor = 0xFF3D5A80.toInt(),
                        ),
                        pagerState = rememberPagerState(initialPage = index, pageCount = { pageSpreads.size }),
                        spreads = pageSpreads,
                        title = "첫 번째 책",
                        pageCount = pageCount,
                        currentPage = page,
                        twoPage = twoPage,
                        posture = posture(constraints.maxWidth, constraints.maxHeight),
                        onBack = {},
                        onDirectionChange = {},
                        direction = direction,
                        overlayVisible = overlay,
                        showTapZoneGuide = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
    }

    private fun capture(file: String) {
        composeRule.onRoot().captureRoboImage(file)
    }
}
