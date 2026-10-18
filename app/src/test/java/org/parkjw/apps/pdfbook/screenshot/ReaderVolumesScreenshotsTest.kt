package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderVolumesScreenshotsTest {
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
    fun bookListLightFolded() = captureBookList(dark = false, file = "reader-book-list-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun bookListDarkFolded() = captureBookList(dark = true, file = "reader-book-list-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun bookListLightUnfolded() = captureBookList(dark = false, file = "reader-book-list-light-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun volumesMiddleLightFolded() = captureReader(
        dark = false,
        file = "reader-volumes-middle-light-folded.png",
        current = 5,
        bookList = false,
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun bookListLightFoldedEn() = captureBookList(dark = false, file = "reader-book-list-light-folded-en.png")

    private fun captureBookList(dark: Boolean, file: String) =
        captureReader(dark = dark, file = file, current = 9, bookList = true)

    private fun captureReader(dark: Boolean, file: String, current: Int, bookList: Boolean) {
        val pageCount = 120
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        captureRoboImage(file) {
            ScreenshotTheme(dark, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(initialPage = 36, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 시리즈 ${current}권",
                    pageCount = pageCount,
                    currentPage = 37,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    volumes = sampleReaderVolumes(current),
                    bookListVisible = bookList,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
