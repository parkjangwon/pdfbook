package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
class ReaderImmersiveComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FakeController : ReaderSystemBarsController {
        val calls = mutableListOf<Boolean>()

        override fun setBarsHidden(hidden: Boolean) {
            calls += hidden
        }
    }

    private fun setReader(
        fake: FakeController,
        fullScreenReading: Boolean = true,
        overlayVisible: Boolean = false,
        widthSheetVisible: Boolean = false,
        readerShown: () -> Boolean = { true },
        routeShown: () -> Boolean = { true },
    ) {
        val pageCount = 4
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            CompositionLocalProvider(LocalReaderSystemBarsController provides fake) {
                PdfBookTheme(themeMode = ThemeMode.Dark) {
                    if (routeShown()) {
                        ReaderSystemBarsScope()
                    }
                    if (readerShown()) {
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
                            overlayVisible = overlayVisible,
                            showTapZoneGuide = false,
                            tapZoneWidthSheetVisible = widthSheetVisible,
                            fullScreenReading = fullScreenReading,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun barsHideWhileReadingAndShowWithOverlay() {
        val fake = FakeController()
        setReader(fake)
        assertEquals(true, fake.calls.last())

        composeRule.onAllNodesWithTag("reader_zone_Menu")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_overlay_top").assertIsDisplayed()
        assertEquals(fake.calls.toString(), false, fake.calls.last())

        composeRule.onAllNodesWithTag("reader_zone_Menu")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(fake.calls.toString(), true, fake.calls.last())
    }

    @Test
    fun settingOffKeepsBarsShown() {
        val fake = FakeController()
        setReader(fake, fullScreenReading = false)
        assertTrue(fake.calls.isNotEmpty())
        assertEquals(false, fake.calls.last())
    }

    @Test
    fun leavingReaderShowsBars() {
        var shown by mutableStateOf(true)
        val fake = FakeController()
        setReader(fake, readerShown = { shown }, routeShown = { shown })
        assertEquals(true, fake.calls.last())

        composeRule.runOnIdle { shown = false }
        composeRule.waitForIdle()
        assertEquals(false, fake.calls.last())
    }

    @Test
    fun switchingBooksKeepsBarsHidden() {
        var screenShown by mutableStateOf(true)
        val fake = FakeController()
        setReader(fake, readerShown = { screenShown })
        assertEquals(true, fake.calls.last())

        composeRule.runOnIdle { screenShown = false }
        composeRule.waitForIdle()
        assertEquals(fake.calls.toString(), true, fake.calls.last())

        composeRule.runOnIdle { screenShown = true }
        composeRule.waitForIdle()
        assertEquals(true, fake.calls.last())
    }

    @Test
    fun openSheetShowsBars() {
        val fake = FakeController()
        setReader(fake, widthSheetVisible = true)
        assertEquals(false, fake.calls.last())
    }
}
