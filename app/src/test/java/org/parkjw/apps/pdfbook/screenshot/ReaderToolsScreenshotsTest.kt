package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReaderTool
import org.parkjw.apps.pdfbook.ReaderToolItem
import org.parkjw.apps.pdfbook.ReaderToolRow
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderToolsScreenshotsTest {
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
    fun toolsLightFolded() = captureOverlay("reader-tools-light-folded.png", dark = false)

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun toolsLightFoldedEn() = captureOverlay("reader-tools-light-folded-en.png", dark = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun toolsDarkFolded() = captureOverlay("reader-tools-dark-folded.png", dark = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun toolsAllLightFolded() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(false, ReaderBackground) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(ReaderOverlay)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("|<  slider  >|", color = ReaderOnOverlay)
                    // Placeholder labels/icons for spacing review; later PRs own real copy/icons.
                    // (Unit-test res is not on R, so literals stay in this test-only capture.)
                    ReaderToolRow(
                        items = listOf(
                            ReaderToolItem.Action(
                                tool = ReaderTool.Brightness,
                                iconRes = R.drawable.ic_brightness,
                                label = "밝기",
                                testTag = "reader_brightness",
                                onClick = {},
                            ),
                            ReaderToolItem.Action(
                                tool = ReaderTool.AutoTurn,
                                iconRes = R.drawable.ic_brightness,
                                label = "자동 넘김",
                                testTag = "reader_auto_turn",
                                onClick = {},
                            ),
                            ReaderToolItem.Action(
                                tool = ReaderTool.Bookmarks,
                                iconRes = R.drawable.ic_bookmark_list,
                                label = "책갈피",
                                testTag = "reader_bookmark_list_button",
                                onClick = {},
                            ),
                            ReaderToolItem.Toggle(
                                tool = ReaderTool.AutoCrop,
                                iconRes = R.drawable.ic_brightness,
                                label = "자동 자르기",
                                testTag = "reader_auto_crop",
                                checked = true,
                                onCheckedChange = {},
                            ),
                        ),
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage("reader-tools-all-light-folded.png")
    }

    private fun captureOverlay(file: String, dark: Boolean) {
        val pageCount = 12
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 책",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.RTL,
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    volumes = sampleReaderVolumes(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage(file)
    }
}
