package org.parkjw.apps.pdfbook.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class EInkModeSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun eInkModeIsOffByDefault() {
        assertFalse(AppSettings().eInkMode)
    }

    @Test
    fun eInkModePersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        assertFalse(repo.settings.first().eInkMode)
        repo.update { it.copy(eInkMode = true) }
        assertTrue(repo.settings.first().eInkMode)
        repo.update { it.copy(eInkMode = false) }
        assertFalse(repo.settings.first().eInkMode)
    }

    @Test
    fun shouldAnimatePageTurnDecisionFunction() {
        // When e-ink mode is off, follow pageTurnAnimation setting
        assertTrue(shouldAnimatePageTurn(eInkMode = false, pageTurnAnimation = PageTurnAnimation.Slide))
        assertFalse(shouldAnimatePageTurn(eInkMode = false, pageTurnAnimation = PageTurnAnimation.None))

        // When e-ink mode is on, page turn animation is always off (instant)
        assertFalse(shouldAnimatePageTurn(eInkMode = true, pageTurnAnimation = PageTurnAnimation.Slide))
        assertFalse(shouldAnimatePageTurn(eInkMode = true, pageTurnAnimation = PageTurnAnimation.None))
    }

    @Test
    fun readingSettingsSwitchShowsDescriptionAndToggles() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
        composeRule.onNodeWithText("전자잉크 모드").performScrollTo().assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithText("넘김·표시 애니메이션을 꺼서 깜빡임을 줄여요.").assertIsDisplayed()
        composeRule.onNodeWithText("전자잉크 모드").performClick()
        composeRule.onNodeWithText("전자잉크 모드").assertIsOn()
        assertTrue(settings.eInkMode)
        composeRule.onNodeWithText("전자잉크 모드").performClick()
        assertFalse(settings.eInkMode)
    }

    @Test
    fun eInkModeTurnsPageInstantly() {
        var pagerState: PagerState? = null
        val pageCount = 6
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            val state = rememberPagerState(pageCount = { pageSpreads.size })
            pagerState = state
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0, 0),
                    pagerState = state,
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.LTR,
                    eInkMode = true,
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        composeRule.mainClock.advanceTimeBy(16)
        assertEquals(1, pagerState?.targetPage)
        assertEquals(1, pagerState?.currentPage)
    }

    @Test
    fun eInkModeOffAnimatesPageTurn() {
        var pagerState: PagerState? = null
        val pageCount = 6
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        composeRule.setContent {
            val state = rememberPagerState(pageCount = { pageSpreads.size })
            pagerState = state
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(pageCount, 0, 0),
                    pagerState = state,
                    spreads = pageSpreads,
                    title = "t",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    direction = ReadingDirection.LTR,
                    eInkMode = false,
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        composeRule.mainClock.advanceTimeBy(16)
        assertEquals(1, pagerState?.targetPage)
        assertEquals(0, pagerState?.currentPage)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("전자잉크 모드", localized(Locale.KOREAN, R.string.settings_e_ink_mode))
        assertEquals(
            "넘김·표시 애니메이션을 꺼서 깜빡임을 줄여요.",
            localized(Locale.KOREAN, R.string.settings_e_ink_mode_desc),
        )
        assertEquals("E-ink mode", localized(Locale.ENGLISH, R.string.settings_e_ink_mode))
        assertEquals(
            "Turns off page-turn and overlay animations to reduce flicker.",
            localized(Locale.ENGLISH, R.string.settings_e_ink_mode_desc),
        )
    }

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
