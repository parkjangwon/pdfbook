package org.parkjw.apps.pdfbook.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PdfBookSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionAndDismissInvokeSnackbarData() {
        var actions = 0
        var dismisses = 0
        val data = actionSnackbarData(
            onAction = { actions += 1 },
            onDismiss = { dismisses += 1 },
        )
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

    @Test
    fun actionLabelUsesInverseOnSurfaceInLight() {
        assertActionInk(ThemeMode.Light)
    }

    @Test
    fun actionLabelUsesInverseOnSurfaceInDark() {
        assertActionInk(ThemeMode.Dark)
    }

    private fun assertActionInk(themeMode: ThemeMode) {
        val ink = arrayOf(Color.Unspecified)
        val primary = arrayOf(Color.Unspecified)
        val inversePrimary = arrayOf(Color.Unspecified)
        composeRule.setContent {
            PdfBookTheme(themeMode = themeMode) {
                ink[0] = MaterialTheme.colorScheme.inverseOnSurface
                primary[0] = MaterialTheme.colorScheme.primary
                inversePrimary[0] = MaterialTheme.colorScheme.inversePrimary
                PdfBookSnackbar(snackbarData = actionSnackbarData())
            }
        }
        val pixels = composeRule.onNodeWithText("Undo").captureToImage().toPixelMap()
        var inkHits = 0
        var orangeHits = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (pixel.alpha < 0.5f) continue
                if (near(pixel, ink[0])) inkHits += 1
                if (near(pixel, primary[0]) || near(pixel, inversePrimary[0])) orangeHits += 1
            }
        }
        assertTrue("action ink missing for $themeMode (hits=$inkHits)", inkHits > 0)
        assertFalse("orange action ink for $themeMode (hits=$orangeHits)", orangeHits > 0)
    }

    private fun near(a: Color, b: Color, tolerance: Float = 0.08f): Boolean {
        return kotlin.math.abs(a.red - b.red) <= tolerance &&
            kotlin.math.abs(a.green - b.green) <= tolerance &&
            kotlin.math.abs(a.blue - b.blue) <= tolerance
    }

    private fun actionSnackbarData(
        onAction: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ): SnackbarData = object : SnackbarData {
        override val visuals = object : SnackbarVisuals {
            override val message = "Saved"
            override val actionLabel = "Undo"
            override val withDismissAction = true
            override val duration = SnackbarDuration.Indefinite
        }

        override fun performAction() = onAction()

        override fun dismiss() = onDismiss()
    }
}
