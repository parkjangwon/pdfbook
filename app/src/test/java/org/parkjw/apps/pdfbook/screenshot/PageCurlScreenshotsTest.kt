package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.PageCurlOverlay
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlScreenshotsTest {
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
    fun settingsAnimationCurlLightFolded() {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsScreen(
                    settings = AppSettings(pageTurnAnimation = PageTurnAnimation.Curl),
                    onSettingsChange = {},
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowAnimationChoice = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("settings-animation-curl-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun settingsAnimationCurlLightFoldedEn() {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsScreen(
                    settings = AppSettings(
                        themeMode = ThemeMode.Light,
                        pageTurnAnimation = PageTurnAnimation.Curl,
                    ),
                    onSettingsChange = {},
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowAnimationChoice = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("settings-animation-curl-light-folded-en.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidLightFolded() {
        val front = solidPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val back = solidPage(0xFFE8F0F7.toInt(), 0xFF80A03D.toInt())
        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ReaderBackground),
                ) {
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = 0.45f,
                        side = CurlSide.Left,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("reader-page-curl-mid-light-folded.png")
    }

    private fun solidPage(pageColor: Int, accentColor: Int): Bitmap {
        val width = 360
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(pageColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        val margin = width / 8f
        canvas.drawRect(margin, margin, width - margin, height / 3f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(width / 2f, height * 0.65f, width / 5f, paint)
        return bitmap
    }
}
