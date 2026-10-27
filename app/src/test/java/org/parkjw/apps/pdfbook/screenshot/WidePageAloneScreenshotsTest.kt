package org.parkjw.apps.pdfbook.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.IntRect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.FoldPosture
import org.parkjw.apps.pdfbook.PageBitmaps
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.spreadIndexOf
import org.parkjw.apps.pdfbook.spreads
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class WidePageAloneScreenshotsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

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
    fun settingsOff() = captureSettings("settings-wide-page-alone-off.png", on = false, dark = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsOn() = captureSettings("settings-wide-page-alone-on.png", on = true, dark = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsDark() = captureSettings("settings-wide-page-alone-dark.png", on = true, dark = true)

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun settingsEnglish() = captureSettings("settings-wide-page-alone-en.png", on = true, dark = false)

    @Test
    @Config(qualifiers = "ko-w320dp-h920dp-xxhdpi", fontScale = 2f)
    fun settings320Font2() = captureSettings("settings-wide-page-alone-320-font2.png", on = true, dark = false)

    // Landscape tablet (w841dp-h701dp): the real two-page target in Auto.

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapeOffLtr() =
        captureSpread("reader-wide-page-alone-w841-off-ltr.png", ReadingDirection.LTR, page = 6, alone = false)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapeOnLtr() = captureSpread("reader-wide-page-alone-w841-on-ltr.png", ReadingDirection.LTR, page = 6)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapePairLtr() = captureSpread("reader-wide-page-alone-w841-pair-ltr.png", ReadingDirection.LTR, page = 7)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapeOffRtl() =
        captureSpread("reader-wide-page-alone-w841-off-rtl.png", ReadingDirection.RTL, page = 6, alone = false)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapeOnRtl() = captureSpread("reader-wide-page-alone-w841-on-rtl.png", ReadingDirection.RTL, page = 6)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun landscapePairRtl() = captureSpread("reader-wide-page-alone-w841-pair-rtl.png", ReadingDirection.RTL, page = 7)

    // #147 shift on, wide page 4: [3] and [4] are two lone spreads in a row.

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun shiftPageThreeLtr() =
        captureSpread("reader-wide-page-alone-w841-shift-3-ltr.png", ReadingDirection.LTR, page = 3, widePage = 4, shift = 1)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun shiftPageFourLtr() =
        captureSpread("reader-wide-page-alone-w841-shift-4-ltr.png", ReadingDirection.LTR, page = 4, widePage = 4, shift = 1)

    // Unfolded 656dp: the other width where Auto turns two-page view on.

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun unfoldedOff() =
        captureSpread("reader-wide-page-alone-unfolded-off.png", ReadingDirection.LTR, page = 6, alone = false)

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun unfoldedOn() = captureSpread("reader-wide-page-alone-unfolded-on.png", ReadingDirection.LTR, page = 6)

    // Book posture draws the lone wide page across both halves, cut at the hinge.

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun bookPostureLtr() =
        captureSpread("reader-wide-page-alone-book-ltr.png", ReadingDirection.LTR, page = 6, book = true)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun bookPostureRtl() =
        captureSpread("reader-wide-page-alone-book-rtl.png", ReadingDirection.RTL, page = 6, book = true)

    @Test
    @Config(qualifiers = FOLDABLE_QUALIFIERS)
    fun bookPostureOffLtr() =
        captureSpread("reader-wide-page-alone-book-off-ltr.png", ReadingDirection.LTR, page = 6, alone = false, book = true)

    // Display modes use the same page bitmaps.

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun dark() = captureSpread("reader-wide-page-alone-dark.png", ReadingDirection.RTL, page = 6, dark = true)

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun inverted() =
        captureSpread("reader-wide-page-alone-inverted.png", ReadingDirection.LTR, page = 6, displayMode = DisplayMode.Invert)

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun sepia() =
        captureSpread("reader-wide-page-alone-sepia.png", ReadingDirection.LTR, page = 6, displayMode = DisplayMode.Sepia)

    private fun captureSettings(file: String, on: Boolean, dark: Boolean) {
        composeRule.setContent {
            ScreenshotTheme(dark = dark) {
                SettingsScreen(
                    settings = sampleSettings().copy(
                        widePageAlone = on,
                        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                    ),
                    onSettingsChange = {},
                    versionName = "0.8.0",
                    versionCode = 8L,
                    modifier = Modifier.fillMaxSize(),
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
        composeRule.onNodeWithText(app.getString(R.string.settings_wide_page_alone)).performScrollTo()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }

    private fun captureSpread(
        file: String,
        direction: ReadingDirection,
        page: Int,
        dark: Boolean = false,
        widePage: Int = WIDE_PAGE,
        alone: Boolean = true,
        shift: Int = 0,
        displayMode: DisplayMode = DisplayMode.Default,
        book: Boolean = false,
    ) {
        val pageCount = 14
        val pageSpreads = spreads(
            pageCount,
            twoPage = true,
            coverAlone = true,
            shift = shift,
            widePages = if (alone) setOf(widePage) else emptySet(),
        )
        val index = spreadIndexOf(pageSpreads, page)
        composeRule.setContent {
            ScreenshotTheme(dark = dark) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val posture = if (book) {
                        val mid = constraints.maxWidth / 2
                        FoldPosture.Book(IntRect(mid - 8, 0, mid + 8, constraints.maxHeight))
                    } else {
                        FoldPosture.Flat
                    }
                    ReaderScreen(
                        renderer = WideNumberedPageBitmaps(pageCount, widePage),
                        pagerState = rememberPagerState(initialPage = index, pageCount = { pageSpreads.size }),
                        spreads = pageSpreads,
                        title = "바닷가 마을 이야기",
                        pageCount = pageCount,
                        currentPage = page,
                        twoPage = true,
                        posture = posture,
                        onBack = {},
                        onDirectionChange = {},
                        direction = direction,
                        displayMode = displayMode,
                        widePages = if (alone) setOf(widePage) else emptySet(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }

    /** [widePage] always renders landscape, so the off capture shows the same book paired. */
    private class WideNumberedPageBitmaps(
        override val pageCount: Int,
        private val widePage: Int,
    ) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
            val wide = page == widePage
            val width = if (wide) {
                maxWidth.coerceAtMost(960).coerceAtLeast(1)
            } else {
                maxWidth.coerceAtMost(480).coerceAtLeast(1)
            }
            val height = if (wide) {
                (width * 9 / 16).coerceAtMost(maxHeight).coerceAtLeast(1)
            } else {
                maxHeight.coerceAtMost(720).coerceAtLeast(1)
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(0xFFF7F2E8.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF2B2B2B.toInt()
                textSize = height / 3f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(page.toString(), width / 2f, height / 2f + paint.textSize / 3f, paint)
            return bitmap
        }
    }

    private companion object {
        const val WIDE_PAGE = 6
    }
}
