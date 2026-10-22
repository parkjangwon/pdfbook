package org.parkjw.apps.pdfbook.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class FullScreenReadingSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun fullScreenReadingIsOnByDefault() {
        assertTrue(AppSettings().fullScreenReading)
    }

    @Test
    fun fullScreenReadingPersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        assertTrue(repo.settings.first().fullScreenReading)
        repo.update { it.copy(fullScreenReading = false) }
        assertFalse(repo.settings.first().fullScreenReading)
        repo.update { it.copy(fullScreenReading = true) }
        assertTrue(repo.settings.first().fullScreenReading)
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
        composeRule.onNodeWithText("전체 화면으로 읽기").performScrollTo().assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithText("읽는 동안 상태 표시줄과 내비게이션 바를 숨겨요.").assertIsDisplayed()
        composeRule.onNodeWithText("전체 화면으로 읽기").performClick()
        composeRule.onNodeWithText("전체 화면으로 읽기").assertIsOff()
        assertFalse(settings.fullScreenReading)
        composeRule.onNodeWithText("전체 화면으로 읽기").performClick()
        assertTrue(settings.fullScreenReading)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("전체 화면으로 읽기", localized(Locale.KOREAN, R.string.settings_full_screen_reading))
        assertEquals(
            "읽는 동안 상태 표시줄과 내비게이션 바를 숨겨요.",
            localized(Locale.KOREAN, R.string.settings_full_screen_reading_desc),
        )
        assertEquals("Full screen reading", localized(Locale.ENGLISH, R.string.settings_full_screen_reading))
        assertEquals(
            "Hides the status and navigation bars while reading.",
            localized(Locale.ENGLISH, R.string.settings_full_screen_reading_desc),
        )
    }

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
