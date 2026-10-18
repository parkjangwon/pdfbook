package org.parkjw.apps.pdfbook.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class SettingsAboutUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsFormattedVersion() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "1.2.3",
                )
            }
        }
        composeRule.onNodeWithText("버전").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("v1.2.3 (0)").assertIsDisplayed()
    }

    @Test
    fun showsVersionCodeWhenProvided() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "1.2.3",
                    versionCode = 9L,
                )
            }
        }
        composeRule.onNodeWithText("v1.2.3 (9)").performScrollTo().assertIsDisplayed()
    }
}
