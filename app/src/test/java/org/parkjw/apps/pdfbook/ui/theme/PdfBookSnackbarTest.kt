package org.parkjw.apps.pdfbook.ui.theme

import android.content.Context
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PdfBookSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionAndDismissInvokeSnackbarData() {
        var actions = 0
        var dismisses = 0
        val data = object : SnackbarData {
            override val visuals = object : SnackbarVisuals {
                override val message = "Saved"
                override val actionLabel = "Undo"
                override val withDismissAction = true
                override val duration = SnackbarDuration.Indefinite
            }

            override fun performAction() {
                actions += 1
            }

            override fun dismiss() {
                dismisses += 1
            }
        }
        val dismissLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.snackbar_dismiss)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                PdfBookSnackbar(snackbarData = data)
            }
        }
        composeRule.onNodeWithText("Undo").performClick()
        assertEquals(1, actions)
        composeRule.onNodeWithContentDescription(dismissLabel).performClick()
        assertEquals(1, dismisses)
    }
}
