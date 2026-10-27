package org.parkjw.apps.pdfbook.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.PageBitmaps
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.R
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
    @Config(qualifiers = "ko-w320dp-h920dp-xxhdpi", fontScale = 2f)
    fun settings320Font2() = captureSettings("settings-wide-page-alone-320-font2.png", on = true, dark = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun spreadLtr() = captureSpread("reader-wide-page-alone-ltr.png", ReadingDirection.LTR, page = 6, dark = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun spreadRtl() = captureSpread("reader-wide-page-alone-rtl.png", ReadingDirection.RTL, page = 6, dark = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pairAfterWidePageRtl() =
        captureSpread("reader-wide-page-alone-pair-rtl.png", ReadingDirection.RTL, page = 7, dark = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun spreadDark() = captureSpread("reader-wide-page-alone-dark.png", ReadingDirection.RTL, page = 6, dark = true)

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

    private fun captureSpread(file: String, direction: ReadingDirection, page: Int, dark: Boolean) {
        val pageCount = 14
        val widePages = setOf(WIDE_PAGE)
        val pageSpreads = spreads(pageCount, twoPage = true, coverAlone = true, widePages = widePages)
        val index = spreadIndexOf(pageSpreads, page)
        composeRule.setContent {
            ScreenshotTheme(dark = dark) {
                ReaderScreen(
                    renderer = WideNumberedPageBitmaps(pageCount, widePages),
                    pagerState = rememberPagerState(initialPage = index, pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "바닷가 마을 이야기",
                    pageCount = pageCount,
                    currentPage = page,
                    twoPage = true,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }

    private class WideNumberedPageBitmaps(
        override val pageCount: Int,
        private val widePages: Set<Int>,
    ) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
            val wide = page in widePages
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
