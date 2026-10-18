package org.parkjw.apps.pdfbook.settings

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.TAP_ZONE_WIDTH_SHEET_TAG
import org.parkjw.apps.pdfbook.lock.AppLockMode
import org.parkjw.apps.pdfbook.lock.AppLockSettingsMenu
import org.parkjw.apps.pdfbook.lock.LockConfig
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class SettingsBUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun text(id: Int) = app.getString(id)

    private val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    @Test
    fun sb_sectionsInOrder() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    versionCode = 4L,
                )
            }
        }
        val sections = listOf(
            text(R.string.settings_section_general),
            text(R.string.settings_section_reading),
            text(R.string.settings_section_storage),
            text(R.string.settings_section_security),
            text(R.string.settings_section_update),
            text(R.string.settings_section_about),
        )
        sections.forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
        val ordered = sections.map { label ->
            composeRule.onNodeWithText(label).fetchSemanticsNode().positionInRoot.y
        }
        assertEquals(ordered.sorted(), ordered)
    }

    @Test
    fun sb_startScreenRowOpensChoiceAndSaves() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.3.0",
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_start_screen)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_start_last_book)).assertIsDisplayed().performClick()
        assertEquals(StartScreen.LastBook, settings.startScreen)
        composeRule.onNodeWithText(text(R.string.settings_start_last_book)).assertIsDisplayed()
    }

    @Test
    fun sb_languageRowOpensChoiceAndSaves() {
        var settings by mutableStateOf(AppSettings())
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.3.0",
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_language)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_language_english)).assertIsDisplayed().performClick()
        assertEquals(AppLanguage.English, settings.language)
    }

    @Test
    fun sb_themeSegmentStaysInline() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_theme)).assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.settings_theme_system))[0].assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_theme_light)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_theme_dark)).assertIsDisplayed()
    }

    @Test
    fun sb_readingEntryOpensSubScreenWithAllReadingSettings() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_reading_entry)).performClick()
        listOf(
            R.string.settings_default_direction,
            R.string.settings_two_page,
            R.string.settings_cover_alone,
            R.string.settings_page_turn_animation,
            R.string.settings_volume_key_paging,
            R.string.settings_keep_screen_on,
            R.string.settings_tap_zone_guide,
            R.string.reader_tap_width_title,
        ).forEach { id ->
            composeRule.onNodeWithText(text(id)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun sb_tapWidthRowShowsPercentAndSheetSavesSteppedValue() {
        var settings by mutableStateOf(AppSettings(tapZoneSidePercent = 33))
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = { settings = it(settings) },
                    versionName = "0.3.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
        composeRule.onNodeWithText("33%").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.reader_tap_width_title)).performClick()
        composeRule.onNodeWithTag(TAP_ZONE_WIDTH_SHEET_TAG).assertIsDisplayed()
        composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(41.2f) }
        composeRule.waitForIdle()
        assertEquals(40, settings.tapZoneSidePercent)
        assertTrue(settings.tapZoneSidePercent in 20..45)
        assertEquals(0, settings.tapZoneSidePercent % 5)
        composeRule.onNodeWithText(text(R.string.reader_tap_width_done)).performClick()
        composeRule.onNodeWithText("40%").assertIsDisplayed()
    }

    @Test
    fun sb_aboutShowsVersionAndCode() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    versionCode = 4L,
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_version)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("v0.3.0 (4)").assertIsDisplayed()
    }

    @Test
    fun sb_openSourceScreenListsLibraries() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    initialSubScreen = SettingsSubScreen.OpenSource,
                )
            }
        }
        OpenSourceLibraries.forEach { entry ->
            composeRule.onNodeWithText(text(entry.name)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onAllNodesWithText(text(R.string.oss_androidx_license)).fetchSemanticsNodes()
            .let { assertTrue(it.isNotEmpty()) }
        composeRule.onAllNodesWithText(text(R.string.oss_jetbrains_mono_license)).assertCountEquals(2)
        composeRule.onNodeWithText(text(R.string.oss_pdfbook_sans_name)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sb_githubRowOpensRepoUrl() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    versionCode = 4L,
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_github)).performScrollTo().performClick()
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.categories.contains(Intent.CATEGORY_BROWSABLE))
        assertEquals("https://github.com/parkjangwon/pdfbook", intent.dataString)
    }

    @Test
    fun sb_backFromSubScreenReturnsToSettings() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_reading_entry)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_default_direction)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.reader_back)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_reading_entry)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_open_source)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.oss_androidx_name)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.reader_back)).performClick()
        composeRule.onNodeWithText(text(R.string.settings_section_about)).performScrollTo().assertIsDisplayed()
    }
}
