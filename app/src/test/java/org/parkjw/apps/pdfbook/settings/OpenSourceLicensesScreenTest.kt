package org.parkjw.apps.pdfbook.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "w411dp-h891dp")
class OpenSourceLicensesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun setScreen(onBack: () -> Unit) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) { OpenSourceLicensesScreen(onBack = onBack) }
        }
    }

    @Test
    fun everyLibraryShowsNameLicenseCopyrightAndUrl() {
        setScreen {}
        OpenSourceLibraries.forEach { entry ->
            for (res in listOf(entry.name, entry.copyright, entry.url)) {
                // copyright and url text is shared by several libraries
                composeRule.onAllNodesWithText(context.getString(res)).onFirst().performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun libraryListHasNoBlankEntries() {
        OpenSourceLibraries.forEach { entry ->
            for (res in listOf(entry.name, entry.license, entry.copyright, entry.url)) {
                assertTrue(context.getString(res).isNotBlank())
            }
        }
    }

    @Test
    fun backButtonAndSystemBackBothCallOnBack() {
        var backs = 0
        setScreen { backs++ }
        composeRule.onNodeWithContentDescription(context.getString(R.string.reader_back)).performClick()
        assertEquals(1, backs)
        Espresso.pressBack()
        composeRule.waitForIdle()
        assertEquals(2, backs)
    }
}
