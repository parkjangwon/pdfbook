package org.parkjw.apps.pdfbook.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
        var settings = AppSettings()
        composeRule.setContent {
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
        composeRule.onNodeWithText(label).performClick()
        assertEquals(true, settings.showDotFiles)
    }
}
