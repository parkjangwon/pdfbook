package org.parkjw.apps.pdfbook.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.update.AppVersion
import org.parkjw.apps.pdfbook.update.RELEASES_PAGE_URL
import org.parkjw.apps.pdfbook.update.Release
import org.parkjw.apps.pdfbook.update.ReleaseAsset
import org.parkjw.apps.pdfbook.update.UpdateAction
import org.parkjw.apps.pdfbook.update.UpdateProblem
import org.parkjw.apps.pdfbook.update.UpdateUiState
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class SettingsUpdateUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val release = Release(
        tagName = "v0.3.1",
        version = AppVersion(0, 3, 1),
        name = "v0.3.1",
        htmlUrl = "https://github.com/parkjangwon/pdfbook/releases/tag/v0.3.1",
        notes = "",
        publishedAt = null,
        draft = false,
        prerelease = false,
        assets = emptyList(),
    )

    @Test
    fun autoCheckIsOnByDefaultAndPersists() = runBlocking {
        assertTrue(AppSettings().autoUpdateCheck)
        val repo = SettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = { File(tmp.root, "settings.preferences_pb") }),
        )
        assertTrue(repo.settings.first().autoUpdateCheck)
        repo.update { it.copy(autoUpdateCheck = false) }
        assertFalse(repo.settings.first().autoUpdateCheck)
    }

    @Test
    fun switchTogglesAutoCheck() {
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
        composeRule.onNodeWithText("앱을 켤 때 하루 한 번 확인해요").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("자동으로 업데이트 확인").assertIsOn().performClick()
        composeRule.onNodeWithText("자동으로 업데이트 확인").assertIsOff()
        assertFalse(settings.autoUpdateCheck)
    }

    @Test
    fun manualCheckButtonSendsCheckRegardlessOfSwitch() {
        val actions = mutableListOf<UpdateAction>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(autoUpdateCheck = false),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    onUpdateAction = { actions += it },
                )
            }
        }
        composeRule.onNodeWithText("업데이트 확인").performScrollTo().performClick()
        assertEquals(listOf(UpdateAction.Check), actions)
    }

    @Test
    fun showsLatestAndAvailableResults() {
        var state by mutableStateOf<UpdateUiState>(UpdateUiState.UpToDate)
        val actions = mutableListOf<UpdateAction>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    updateState = state,
                    onUpdateAction = { actions += it },
                )
            }
        }
        composeRule.onNodeWithText("최신 버전이에요").performScrollTo().assertIsDisplayed()
        state = UpdateUiState.Available(release, null, prompt = false)
        composeRule.onNodeWithText("새 버전이 있어요 (0.3.1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("받고 설치하기").performScrollTo().performClick()
        state = UpdateUiState.Downloading(release, 42)
        composeRule.onNodeWithText("받는 중 42%").performScrollTo().assertIsDisplayed()
        assertEquals(listOf(UpdateAction.Download), actions)
    }

    @Test
    fun failuresShowCopyAndReleasePageButton() {
        var state by mutableStateOf<UpdateUiState>(UpdateUiState.Failed(UpdateProblem.Check, RELEASES_PAGE_URL))
        val actions = mutableListOf<UpdateAction>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    updateState = state,
                    onUpdateAction = { actions += it },
                )
            }
        }
        composeRule.onNodeWithText("업데이트를 확인하지 못했어요. 인터넷 연결을 확인해 주세요.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("릴리스 페이지 열기").performScrollTo().performClick()
        state = UpdateUiState.Failed(UpdateProblem.Download, RELEASES_PAGE_URL)
        composeRule.onNodeWithText("파일을 받지 못했어요").performScrollTo().assertIsDisplayed()
        state = UpdateUiState.Failed(UpdateProblem.Verify, RELEASES_PAGE_URL)
        composeRule.onNodeWithText("파일을 확인하지 못해 설치하지 않았어요. 릴리스 페이지에서 받아 주세요.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("릴리스 페이지 열기").performScrollTo().performClick()
        assertEquals(listOf(UpdateAction.OpenReleasePage, UpdateAction.OpenReleasePage), actions)
    }

    @Test
    fun missingInstallPermissionOffersSettings() {
        val actions = mutableListOf<UpdateAction>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SettingsScreen(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    updateState = UpdateUiState.NeedsInstallPermission(
                        release,
                        ReleaseAsset("pdfbook-0.3.1.apk", "https://github.com/x", 1, ""),
                    ),
                    onUpdateAction = { actions += it },
                )
            }
        }
        composeRule.onNodeWithText("설치하려면 PDFBook의 앱 설치를 허용해 주세요").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("설정 열기").performScrollTo().performClick()
        assertEquals(listOf(UpdateAction.OpenSettings), actions)
    }
}
