package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class ReaderAutoCropComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun autoCropStartsOffAndStaysOnAfterLeavingTheReader() {
        val autoCrop = mutableStateOf(false)
        val visible = mutableStateOf(true)
        val pageCount = 4
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                if (visible.value) {
                    ReaderScreen(
                        renderer = SolidPageBitmaps(pageCount, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                        pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                        spreads = pageSpreads,
                        title = "책 1권",
                        pageCount = pageCount,
                        currentPage = 1,
                        twoPage = false,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.RTL,
                        overlayVisible = true,
                        showTapZoneGuide = false,
                        autoCrop = autoCrop.value,
                        onAutoCropChange = { autoCrop.value = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("reader_auto_crop").assertIsOff().performClick()
        composeRule.waitForIdle()
        assertTrue(autoCrop.value)
        composeRule.onNodeWithTag("reader_auto_crop").assertIsOn()

        visible.value = false
        composeRule.waitForIdle()
        visible.value = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_auto_crop").assertIsOn()
    }
}
