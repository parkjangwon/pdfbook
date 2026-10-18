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
import org.parkjw.apps.pdfbook.JUMP_BACK_MILLIS
import org.parkjw.apps.pdfbook.JumpBack
import org.parkjw.apps.pdfbook.NEXT_BOOK_NOTICE_MILLIS
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReaderVolumes
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreadIndexOf
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class JumpBackScreenshotsTest {
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
    fun jumpBackLightFolded() {
        setReader(dark = false, twoPage = false, at = 80, back = 12)
        capture("reader-jump-back-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun jumpBackMenuLightFolded() {
        setReader(dark = false, twoPage = false, at = 80, back = 12, overlay = true)
        capture("reader-jump-back-menu-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun jumpBackRoundTripLightFolded() {
        setReader(dark = false, twoPage = false, at = 12, back = 80)
        capture("reader-jump-back-return-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun jumpBackLightFoldedEn() {
        setReader(dark = false, twoPage = false, at = 80, back = 12)
        capture("reader-jump-back-light-folded-en.png")
    }

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun jumpBackDarkFolded() {
        setReader(dark = true, twoPage = false, at = 80, back = 12)
        capture("reader-jump-back-dark-folded.png")
    }

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun jumpBackDarkUnfolded() {
        setReader(dark = true, twoPage = true, at = 80, back = 12)
        capture("reader-jump-back-dark-unfolded.png")
    }

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun jumpBackMenuLightUnfolded() {
        setReader(dark = false, twoPage = true, at = 80, back = 12, overlay = true)
        capture("reader-jump-back-menu-light-unfolded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun nextBookCardThenJumpBackLightFolded() {
        setReader(dark = false, twoPage = false, at = 120, back = 12, notice = EndNotice.NextBook(nextBook))
        capture("reader-jump-back-after-card-1-light-folded.png")
        composeRule.mainClock.advanceTimeBy(NEXT_BOOK_NOTICE_MILLIS + 500)
        capture("reader-jump-back-after-card-2-light-folded.png")
        composeRule.mainClock.advanceTimeBy(JUMP_BACK_MILLIS + 500)
        capture("reader-jump-back-after-card-3-light-folded.png")
    }

    private fun setReader(
        dark: Boolean,
        twoPage: Boolean,
        at: Int,
        back: Int,
        overlay: Boolean = false,
        notice: EndNotice? = null,
    ) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = twoPage, coverAlone = true)
        val index = spreadIndexOf(pageSpreads, at)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(initialPage = index, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 시리즈 5권",
                    pageCount = pageCount,
                    currentPage = pageSpreads[index].first(),
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = overlay,
                    showTapZoneGuide = false,
                    volumes = ReaderVolumes(
                        onNext = if (notice != null) ({ }) else null,
                        nextBook = (notice as? EndNotice.NextBook)?.book,
                    ),
                    endNotice = notice,
                    jumpBack = JumpBack(page = back, shownAt = index),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
    }

    private fun capture(file: String) {
        composeRule.onRoot().captureRoboImage(file)
    }
}
