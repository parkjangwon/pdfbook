package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.EndNotice
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReaderVolumes
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class NextBookScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val nextBook = BookInfo("content://sample/vol6", "샘플 시리즈 6권.pdf", 1_006L)

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
    fun nextBookLightFolded() = capture(
        dark = false,
        notice = EndNotice.NextBook(nextBook),
        twoPage = false,
        file = "reader-next-book-light-folded.png",
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun noNextBookLightFolded() = capture(
        dark = false,
        notice = EndNotice.NoNextBook,
        twoPage = false,
        file = "reader-no-next-book-light-folded.png",
    )

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun nextBookDarkUnfolded() = capture(
        dark = true,
        notice = EndNotice.NextBook(nextBook),
        twoPage = true,
        file = "reader-next-book-dark-unfolded.png",
    )

    private fun capture(dark: Boolean, notice: EndNotice, twoPage: Boolean, file: String) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(
                        initialPage = pageSpreads.lastIndex,
                        pageCount = { pageSpreads.size },
                    ),
                    spreads = pageSpreads,
                    title = "샘플 시리즈 5권",
                    pageCount = pageCount,
                    currentPage = pageCount,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    showTapZoneGuide = false,
                    volumes = ReaderVolumes(
                        onNext = if (notice is EndNotice.NextBook) ({ }) else null,
                        nextBook = (notice as? EndNotice.NextBook)?.book,
                    ),
                    endNotice = notice,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage(file)
    }
}
