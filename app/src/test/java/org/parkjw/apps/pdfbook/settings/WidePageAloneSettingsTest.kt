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
class WidePageAloneSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun widePageAloneIsOffByDefault() {
        assertFalse(AppSettings().widePageAlone)
    }

    @Test
    fun widePageAlonePersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        assertFalse(repo.settings.first().widePageAlone)
        repo.update { it.copy(widePageAlone = true) }
        assertTrue(repo.settings.first().widePageAlone)
        repo.update { it.copy(widePageAlone = false) }
        assertFalse(repo.settings.first().widePageAlone)
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
        composeRule.onNodeWithText("넓은 쪽은 혼자 보기").performScrollTo().assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithText("가로가 더 긴 쪽은 옆 쪽과 묶지 않고 한 화면을 다 써요")
            .assertIsDisplayed()
        composeRule.onNodeWithText("넓은 쪽은 혼자 보기").performClick()
        composeRule.onNodeWithText("넓은 쪽은 혼자 보기").assertIsOn()
        assertTrue(settings.widePageAlone)
        composeRule.onNodeWithText("넓은 쪽은 혼자 보기").performClick()
        assertFalse(settings.widePageAlone)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("넓은 쪽은 혼자 보기", localized(Locale.KOREAN, R.string.settings_wide_page_alone))
        assertEquals(
            "가로가 더 긴 쪽은 옆 쪽과 묶지 않고 한 화면을 다 써요",
            localized(Locale.KOREAN, R.string.settings_wide_page_alone_desc),
        )
        assertEquals("Show wide pages alone", localized(Locale.ENGLISH, R.string.settings_wide_page_alone))
        assertEquals(
            "Pages wider than tall fill the screen instead of pairing with a neighbor",
            localized(Locale.ENGLISH, R.string.settings_wide_page_alone_desc),
        )
    }

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
