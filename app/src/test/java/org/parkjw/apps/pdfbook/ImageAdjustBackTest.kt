package org.parkjw.apps.pdfbook

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class ImageAdjustBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun systemBackReturnsToReadingSettingsNotOutOfSettings() {
        val store = BookStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { File(tmp.root, "pdfbook-back.preferences_pb") },
            ),
        )
        val book = BookInfo("content://tree/book/a.pdf", "테스트 책.pdf", 1_000L)
        composeRule.setContent {
            PdfBookTheme {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.8.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    bookStore = store,
                    lastBook = book,
                    lastBookPage = 41,
                )
            }
        }
        composeRule.onNodeWithTag("settings_image_adjust_row").performClick()
        composeRule.onNodeWithTag(IMAGE_ADJUST_SCREEN_TAG).assertIsDisplayed()

        Espresso.pressBack()

        composeRule.onNodeWithTag("settings_reading_screen").assertIsDisplayed()
    }
}
