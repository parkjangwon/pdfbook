package org.parkjw.apps.pdfbook

import android.view.View
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class KeepScreenOnTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var view: View
    private lateinit var shown: MutableState<Boolean>
    private lateinit var keep: MutableState<Boolean>

    @Test
    fun readerKeepsScreenOnWhenSettingOn() {
        setReader(keepScreenOn = true)
        composeRule.runOnIdle { assertTrue(view.keepScreenOn) }
    }

    @Test
    fun readerAllowsSystemTimeoutWhenSettingOff() {
        setReader(keepScreenOn = false)
        composeRule.runOnIdle { assertFalse(view.keepScreenOn) }
    }

    @Test
    fun togglingSettingWhileReadingUpdatesTheView() {
        setReader(keepScreenOn = true)
        composeRule.runOnIdle {
            assertTrue(view.keepScreenOn)
            keep.value = false
        }
        composeRule.runOnIdle { assertFalse(view.keepScreenOn) }
        composeRule.runOnIdle { keep.value = true }
        composeRule.runOnIdle { assertTrue(view.keepScreenOn) }
    }

    @Test
    fun leavingReaderRestoresScreenTimeout() {
        setReader(keepScreenOn = true)
        composeRule.runOnIdle {
            assertTrue(view.keepScreenOn)
            shown.value = false
        }
        composeRule.runOnIdle { assertFalse(view.keepScreenOn) }
    }

    private fun setReader(keepScreenOn: Boolean) {
        composeRule.setContent {
            view = LocalView.current
            shown = remember { mutableStateOf(true) }
            keep = remember { mutableStateOf(keepScreenOn) }
            if (shown.value) {
                PdfBookTheme(themeMode = ThemeMode.Light) {
                    val pages = remember { listOf(listOf(1)) }
                    val pagerState = rememberPagerState(pageCount = { pages.size })
                    ReaderScreen(
                        renderer = remember { SolidPageBitmaps(1, 0xFFFFFFFF.toInt(), 0xFF000000.toInt()) },
                        pagerState = pagerState,
                        spreads = pages,
                        title = "책",
                        pageCount = 1,
                        currentPage = 1,
                        twoPage = false,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        showTapZoneGuide = false,
                        keepScreenOn = keep.value,
                    )
                }
            }
        }
    }
}
