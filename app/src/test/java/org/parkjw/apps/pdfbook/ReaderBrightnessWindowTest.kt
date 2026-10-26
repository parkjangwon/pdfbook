package org.parkjw.apps.pdfbook

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class ReaderBrightnessWindowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun enteringReaderWithManualLevelSetsWindowAndLeavingRestoresSystem() {
        val open = mutableStateOf(false)
        setReader(open, followSystem = false, percent = 40)

        composeRule.runOnIdle { assertEquals(brightnessNone(), windowBrightness(), 0f) }

        composeRule.runOnUiThread { open.value = true }
        composeRule.runOnIdle { assertEquals(0.4f, windowBrightness(), 0.001f) }

        composeRule.runOnUiThread { open.value = false }
        composeRule.runOnIdle { assertEquals(brightnessNone(), windowBrightness(), 0f) }
    }

    @Test
    fun changingLevelWhileReaderOpenUpdatesWindow() {
        val open = mutableStateOf(false)
        val level = setReader(open, followSystem = false, percent = 40)

        composeRule.runOnUiThread { open.value = true }
        composeRule.runOnIdle { assertEquals(0.4f, windowBrightness(), 0.001f) }

        composeRule.runOnUiThread { level.intValue = 75 }
        composeRule.runOnIdle { assertEquals(0.75f, windowBrightness(), 0.001f) }
    }

    @Test
    fun readerFollowingSystemLeavesWindowBrightnessAlone() {
        val open = mutableStateOf(false)
        setReader(open, followSystem = true, percent = 40)

        composeRule.runOnUiThread { open.value = true }
        composeRule.runOnIdle { assertEquals(brightnessNone(), windowBrightness(), 0f) }
    }

    private fun setReader(
        open: androidx.compose.runtime.MutableState<Boolean>,
        followSystem: Boolean,
        percent: Int,
    ): androidx.compose.runtime.MutableIntState {
        val level = mutableIntStateOf(percent)
        val pageCount = 3
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            var readerOpen by open
            var brightnessPercent by level
            PdfBookTheme(themeMode = ThemeMode.Light) {
                if (readerOpen) {
                    ReaderScreen(
                        renderer = SolidPageBitmaps(pageCount, 0, 0),
                        pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                        spreads = pageSpreads,
                        title = "t",
                        pageCount = pageCount,
                        currentPage = 1,
                        twoPage = false,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        followSystemBrightness = followSystem,
                        brightnessPercent = brightnessPercent,
                        onBrightnessChange = { brightnessPercent = it },
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        return level
    }

    private fun windowBrightness(): Float = composeRule.activity.window.attributes.screenBrightness

    private fun brightnessNone(): Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
}
