package org.parkjw.apps.pdfbook.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class PageCurlSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun defaultRemainsSlide() {
        assertEquals(PageTurnAnimation.Slide, AppSettings().pageTurnAnimation)
    }

    @Test
    fun curlPersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }),
        )
        assertEquals(PageTurnAnimation.Slide, repo.settings.first().pageTurnAnimation)
        repo.update { it.copy(pageTurnAnimation = PageTurnAnimation.Curl) }
        assertEquals(PageTurnAnimation.Curl, repo.settings.first().pageTurnAnimation)
    }

    @Test
    fun readingSettingsShowsCurlOptionWithDescription() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowAnimationChoice = true,
                )
            }
        }
        composeRule.onNodeWithText("책장 넘김").assertIsDisplayed()
        composeRule.onNodeWithText("한쪽 보기에서만 써요. 두쪽 보기에서는 슬라이드로 넘겨요.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_animation_option_Curl").performClick()
        assertEquals(PageTurnAnimation.Curl, settings.pageTurnAnimation)
        composeRule.onNodeWithText(text(R.string.settings_page_turn_animation))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("책장 넘김").assertIsDisplayed()
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("책장 넘김", localized(Locale.KOREAN, R.string.settings_animation_curl))
        assertEquals(
            "한쪽 보기에서만 써요. 두쪽 보기에서는 슬라이드로 넘겨요.",
            localized(Locale.KOREAN, R.string.settings_animation_curl_desc),
        )
        assertEquals("Page curl", localized(Locale.ENGLISH, R.string.settings_animation_curl))
        assertEquals(
            "Single-page view only. Two-page view uses Slide.",
            localized(Locale.ENGLISH, R.string.settings_animation_curl_desc),
        )
    }

    private fun text(id: Int): String = app.getString(id)

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
