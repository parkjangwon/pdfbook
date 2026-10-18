package org.parkjw.apps.pdfbook.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class StorageSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showDotFilesToggleFlipsOnSettingsChange() {
        val label = ApplicationProvider.getApplicationContext<Context>().getString(R.string.show_dot_files)
        composeRule.setContent {
            var settings by remember { mutableStateOf(AppSettings()) }
            PdfBookTheme(themeMode = ThemeMode.Light) {
                Column(Modifier.fillMaxSize()) {
                    StorageSettings(
                        hiddenCount = 2,
                        showDotFiles = settings.showDotFiles,
                        onOpenHiddenItems = {},
                        onShowDotFilesChange = { checked ->
                            settings = settings.copy(showDotFiles = checked)
                        },
                    )
                }
            }
        }
        composeRule.onNodeWithText(label).assertIsOff()
        composeRule.onNodeWithText(label).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label).assertIsOn()
        composeRule.onNodeWithText(label).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(label).assertIsOff()
    }
}
