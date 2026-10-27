package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi captures for the lone-page turn (#173): the cover forward onto the 2·3 spread and
 * a lone last page backward onto the previous spread, LTR and RTL. Pages are generated
 * synthetic colour pages; no real book, screenshot or private file is used. All captures live
 * in one test method: in this Robolectric setup a second capture-bearing method renders blank.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlCoverScreenshotsTest {
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
    fun coverAndLoneLastPageCurl() {
        val cover = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val lastPage = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF6A4C93.toInt())
        val targetLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())
        // The last-page turn lands on its own previous spread, so the p95 frames differ from
        // the cover turn's (they were byte-identical when both reused the cover's targets).
        val previousLeft = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF8D6E63.toInt())
        val previousRight = syntheticTwoPageCurlPage(0xFFF7F2E8.toInt(), 0xFF00796B.toInt())

        var page by mutableStateOf(cover)
        var side by mutableStateOf(CurlSide.Right)
        var progress by mutableStateOf(0.5f)
        var targetA by mutableStateOf(targetLeft)
        var targetB by mutableStateOf(targetRight)
        var direction by mutableStateOf(ReadingDirection.LTR)
        var mode by mutableStateOf(DisplayMode.Default)
        var night by mutableStateOf(false)

        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                SingleSheetCurlPreview(
                    side = side,
                    progress = progress,
                    page = page,
                    targetLeft = targetA,
                    targetRight = targetB,
                    mode = mode,
                    direction = direction,
                    night = night,
                )
            }
        }
        composeRule.waitForIdle()

        for (p in listOf(0.5f, 0.95f)) {
            composeRule.runOnIdle {
                page = cover
                side = CurlSide.Right
                progress = p
                targetA = targetLeft
                targetB = targetRight
                direction = ReadingDirection.LTR
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-cover-ltr-p${percent(p)}.png")
        }
        for (p in listOf(0.5f, 0.95f)) {
            composeRule.runOnIdle {
                page = cover
                side = CurlSide.Left
                progress = p
                targetA = targetLeft
                targetB = targetRight
                // RTL shows the target spread reversed; the builder places the pages.
                direction = ReadingDirection.RTL
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-cover-rtl-p${percent(p)}.png")
        }
        // A lone last page turns backward, so the side mirrors the forward cover turn.
        for (p in listOf(0.5f, 0.95f)) {
            composeRule.runOnIdle {
                page = lastPage
                side = CurlSide.Left
                progress = p
                targetA = previousLeft
                targetB = previousRight
                direction = ReadingDirection.LTR
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-last-ltr-p${percent(p)}.png")
        }
        for (p in listOf(0.5f, 0.95f)) {
            composeRule.runOnIdle {
                page = lastPage
                side = CurlSide.Right
                progress = p
                targetA = previousLeft
                targetB = previousRight
                direction = ReadingDirection.RTL
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-last-rtl-p${percent(p)}.png")
        }
        // Inverted page mode: the page filter and the inverted paper back the reader passes.
        composeRule.runOnIdle {
            page = cover
            side = CurlSide.Right
            progress = 0.5f
            targetA = targetLeft
            targetB = targetRight
            direction = ReadingDirection.LTR
            mode = DisplayMode.Invert
            night = false
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("reader-page-curl-cover-inverted-p50.png")
        // Night page mode: already-dark pages, the inverted paper/shadow look, no filter.
        composeRule.runOnIdle {
            page = syntheticTwoPageCurlPage(NIGHT_PAGE, NIGHT_ACCENT)
            targetA = syntheticTwoPageCurlPage(NIGHT_PAGE, NIGHT_LEFT)
            targetB = syntheticTwoPageCurlPage(NIGHT_PAGE, NIGHT_RIGHT)
            mode = DisplayMode.Default
            night = true
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("reader-page-curl-cover-night-p50.png")
    }

    private fun percent(p: Float): Int = (p * 100).roundToInt()

    private companion object {
        const val NIGHT_PAGE = 0xFF121212.toInt()
        const val NIGHT_ACCENT = 0xFFCFC6B4.toInt()
        const val NIGHT_LEFT = 0xFF2E7D32.toInt()
        const val NIGHT_RIGHT = 0xFFD9534F.toInt()
    }
}
