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
class KeepZoomSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun keepZoomIsOffByDefault() {
        assertFalse(AppSettings().keepZoomOnTurn)
    }

    @Test
    fun keepZoomPersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        assertFalse(repo.settings.first().keepZoomOnTurn)
        repo.update { it.copy(keepZoomOnTurn = true) }
        assertTrue(repo.settings.first().keepZoomOnTurn)
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
        composeRule.onNodeWithText("넘겨도 확대 유지").performScrollTo().assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithText("확대한 배율 그대로 다음 쪽의 시작 모서리부터 보여 줘요").assertIsDisplayed()
        composeRule.onNodeWithText("넘겨도 확대 유지").performClick()
        composeRule.onNodeWithText("넘겨도 확대 유지").assertIsOn()
        assertTrue(settings.keepZoomOnTurn)
        composeRule.onNodeWithText("넘겨도 확대 유지").performClick()
        assertFalse(settings.keepZoomOnTurn)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("넘겨도 확대 유지", localized(Locale.KOREAN, R.string.settings_keep_zoom))
        assertEquals(
            "확대한 배율 그대로 다음 쪽의 시작 모서리부터 보여 줘요",
            localized(Locale.KOREAN, R.string.settings_keep_zoom_desc),
        )
        assertEquals("Keep zoom when turning pages", localized(Locale.ENGLISH, R.string.settings_keep_zoom))
        assertEquals(
            "Keeps your zoom and starts from the top corner of the next page",
            localized(Locale.ENGLISH, R.string.settings_keep_zoom_desc),
        )
    }

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
