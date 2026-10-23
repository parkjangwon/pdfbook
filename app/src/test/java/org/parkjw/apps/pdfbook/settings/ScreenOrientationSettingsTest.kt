package org.parkjw.apps.pdfbook.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class ScreenOrientationSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun orientationRowOpensChoiceAndSaves() {
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
        composeRule.onNodeWithText(text(R.string.settings_screen_orientation))
            .performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText(text(R.string.settings_orientation_landscape))
            .assertIsDisplayed().performClick()
        assertEquals(ScreenOrientation.Landscape, settings.screenOrientation)
        composeRule.onNodeWithText(text(R.string.settings_screen_orientation))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.settings_orientation_portrait))
            .assertIsDisplayed().performClick()
        assertEquals(ScreenOrientation.Portrait, settings.screenOrientation)
    }

    @Test
    fun stringsMatchSpec() {
        assertEquals("화면 방향", localized(Locale.KOREAN, R.string.settings_screen_orientation))
        assertEquals("자동", localized(Locale.KOREAN, R.string.settings_orientation_auto))
        assertEquals("세로", localized(Locale.KOREAN, R.string.settings_orientation_portrait))
        assertEquals("가로", localized(Locale.KOREAN, R.string.settings_orientation_landscape))
        assertEquals("Screen orientation", localized(Locale.ENGLISH, R.string.settings_screen_orientation))
        assertEquals("Auto", localized(Locale.ENGLISH, R.string.settings_orientation_auto))
        assertEquals("Portrait", localized(Locale.ENGLISH, R.string.settings_orientation_portrait))
        assertEquals("Landscape", localized(Locale.ENGLISH, R.string.settings_orientation_landscape))
    }

    private fun text(id: Int) = app.getString(id)

    private fun localized(locale: Locale, id: Int): String {
        val config = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return app.createConfigurationContext(config).getString(id)
    }
}
