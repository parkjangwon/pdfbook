package org.parkjw.apps.pdfbook.settings

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.PageFit
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageFitSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun rowShowsLastReadBookAndCurrentFit() {
        setReading(
            bookTitle = "원피스 01",
            pageFit = PageFit.Width,
            onPageFitChange = {},
        )
        composeRule.onNodeWithText(app.getString(R.string.settings_page_fit))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.settings_image_adjust_book, "원피스 01"))
            .assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.page_fit_width)).assertIsDisplayed()
    }

    @Test
    fun dialogListsAllThreeFitsWithDescriptionsAndNote() {
        setReading(bookTitle = "원피스 01", pageFit = PageFit.Screen, onPageFitChange = {})
        openDialog()
        composeRule.onNodeWithTag("settings_page_fit_option_Screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_page_fit_option_Width").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_page_fit_option_Height").assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.page_fit_width_desc)).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.page_fit_note_rtl)).assertIsDisplayed()
    }

    @Test
    fun ltrBookShowsTheLeftStartNote() {
        setReading(
            bookTitle = "Book",
            pageFit = PageFit.Screen,
            direction = ReadingDirection.LTR,
            onPageFitChange = {},
        )
        openDialog()
        composeRule.onNodeWithText(app.getString(R.string.page_fit_note_ltr)).assertIsDisplayed()
    }

    @Test
    fun pickingHeightReportsIt() {
        var picked: PageFit? = null
        setReading(bookTitle = "원피스 01", pageFit = PageFit.Screen, onPageFitChange = { picked = it })
        openDialog()
        composeRule.onNodeWithTag("settings_page_fit_option_Height").performClick()
        assertEquals(PageFit.Height, picked)
        composeRule.onNodeWithTag("settings_page_fit_option_Height").assertDoesNotExist()
    }

    @Test
    fun noLastBookDisablesTheRow() {
        setReading(bookTitle = null, pageFit = PageFit.Screen, onPageFitChange = {})
        composeRule.onNodeWithText(app.getString(R.string.settings_page_fit_no_book))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.settings_page_fit))
            .assertIsNotEnabled()
    }

    @Test
    @Config(sdk = [37], qualifiers = "ko-w320dp-h800dp-mdpi")
    fun dialogScrollsAt320dpAndFontScale2() {
        // The dialog composes in its own window, so the scale has to come from the configuration.
        RuntimeEnvironment.setFontScale(2f)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.8.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    pageFitBookTitle = "등대지기의 편지 2권",
                )
            }
        }
        openDialog()
        composeRule.onNodeWithTag("settings_page_fit_option_Height").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.page_fit_note_rtl)).performScrollTo().assertIsDisplayed()
        val subtitle = composeRule.onNodeWithTag("settings_dialog_subtitle")
        subtitle.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        subtitle.fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        val layout = layouts.first()
        // The subtitle used to be maxLines=1 + Ellipsis; it must be free to wrap in the dialog.
        assertEquals(Int.MAX_VALUE, layout.layoutInput.maxLines)
        assertEquals(TextOverflow.Clip, layout.layoutInput.overflow)
        assertEquals(
            app.getString(R.string.settings_image_adjust_book, "등대지기의 편지 2권"),
            layout.layoutInput.text.text,
        )
        // One 12sp line is ~16px at mdpi, so this also proves the configuration font scale arrived.
        assertTrue(layout.size.height > 28)
    }

    private fun openDialog() {
        composeRule.onNodeWithText(app.getString(R.string.settings_page_fit))
            .performScrollTo()
            .performClick()
    }

    private fun setReading(
        bookTitle: String?,
        pageFit: PageFit,
        direction: ReadingDirection = ReadingDirection.RTL,
        onPageFitChange: (PageFit) -> Unit,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.8.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    pageFitBookTitle = bookTitle,
                    pageFit = pageFit,
                    pageFitDirection = direction,
                    onPageFitChange = onPageFitChange,
                )
            }
        }
    }
}
