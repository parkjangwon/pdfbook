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
class OneFingerZoomSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun oneFingerZoomIsOffByDefault() {
        assertFalse(AppSettings().oneFingerZoom)
    }

    @Test
    fun oneFingerZoomPersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        assertFalse(repo.settings.first().oneFingerZoom)
        repo.update { it.copy(oneFingerZoom = true) }
        assertTrue(repo.settings.first().oneFingerZoom)
    }

    @Test
    fun readingSettingsSwitchShowsDescriptionAndToggles() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.8.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
        composeRule.onNodeWithText("한 손가락 확대").performScrollTo().assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithText("두 번 탭한 뒤 누르고 있는 동안만 확대하고, 떼면 원래 크기로 돌아가요")
            .assertIsDisplayed()
        composeRule.onNodeWithText("한 손가락 확대").performClick()
        composeRule.onNodeWithText("한 손가락 확대").assertIsOn()
        assertTrue(settings.oneFingerZoom)
        composeRule.onNodeWithText("한 손가락 확대").performClick()
        assertFalse(settings.oneFingerZoom)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("한 손가락 확대", localized(Locale.KOREAN, R.string.settings_one_finger_zoom))
        assertEquals(
            "두 번 탭한 뒤 누르고 있는 동안만 확대하고, 떼면 원래 크기로 돌아가요",
            localized(Locale.KOREAN, R.string.settings_one_finger_zoom_desc),
        )
        assertEquals("One-finger zoom", localized(Locale.ENGLISH, R.string.settings_one_finger_zoom))
        assertEquals(
            "Double-tap and hold to zoom in, then release to zoom back out",
            localized(Locale.ENGLISH, R.string.settings_one_finger_zoom_desc),
        )
    }

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
