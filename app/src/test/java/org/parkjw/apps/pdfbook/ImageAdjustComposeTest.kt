package org.parkjw.apps.pdfbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class ImageAdjustComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val book = BookInfo("content://tree/book/a.pdf", "테스트 책.pdf", 1_000L)

    private fun store(): BookStore = BookStore(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(tmp.root, "pdfbook-test.preferences_pb") },
        ),
    )

    private fun settings(store: BookStore?, book: BookInfo?) {
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
    }

    @Test
    fun rowSitsInReadingSettingsWithOffAndTheLastBook() {
        settings(store(), book)
        composeRule.onNodeWithTag("settings_image_adjust_row").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("이미지 보정").assertIsDisplayed()
        composeRule.onNodeWithText("마지막 읽던 책: 테스트 책").assertIsDisplayed()
        composeRule.onNodeWithText("꺼짐").assertIsDisplayed()
    }

    @Test
    fun rowShowsOnWhenThisBookHasAdjustment() {
        val store = store()
        runBlocking { store.saveAdjust(bookKey(book.uri, book.size), ImageAdjust(contrast = 30)) }
        settings(store, book)
        composeRule.onNodeWithText("켜짐").assertIsDisplayed()
    }

    @Test
    fun rowIsDisabledWithoutAReadBook() {
        settings(store(), null)
        composeRule.onNodeWithTag("settings_image_adjust_row").assertIsNotEnabled()
        composeRule.onNodeWithText("먼저 책을 열어 주세요").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_image_adjust_row").performClick()
        composeRule.onNodeWithTag(IMAGE_ADJUST_SCREEN_TAG).assertDoesNotExist()
    }

    @Test
    fun rowOpensTheAdjustScreen() {
        settings(store(), book)
        composeRule.onNodeWithTag("settings_image_adjust_row").performClick()
        composeRule.onNodeWithTag(IMAGE_ADJUST_SCREEN_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("이미지 보정").assertIsDisplayed()
        composeRule.onNodeWithTag(IMAGE_ADJUST_GAMMA_TAG).assertIsEnabled()
    }

    @Test
    fun slidersChangeAndResetTheAdjustment() {
        var adjust by mutableStateOf(ImageAdjust.Default)
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = adjust,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = SolidPageBitmaps(3, android.graphics.Color.WHITE, android.graphics.Color.GRAY),
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = true,
                    onAdjustChange = { adjust = it },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(IMAGE_ADJUST_CONTRAST_TAG).performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        composeRule.runOnIdle { assertEquals(30, adjust.contrast) }

        composeRule.onNodeWithTag(IMAGE_ADJUST_GRAYSCALE_TAG).performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
        composeRule.runOnIdle { assertEquals(60, adjust.grayscale) }
        composeRule.onNodeWithTag(IMAGE_ADJUST_RESET_TAG).performScrollTo().assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(ImageAdjust.Default, adjust) }
        composeRule.onNodeWithTag(IMAGE_ADJUST_RESET_TAG).assertIsNotEnabled()
    }

    @Test
    fun gammaRowIsDisabledBelowApi36WithTheHelperText() {
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = ImageAdjust.Default,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = null,
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = false,
                    onAdjustChange = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(IMAGE_ADJUST_GAMMA_TAG).assertIsNotEnabled()
        composeRule.onNodeWithText("Android 16 이상에서 쓸 수 있어요").assertIsDisplayed()
        composeRule.onNodeWithTag(IMAGE_ADJUST_BRIGHTNESS_TAG).assertIsEnabled()
    }

    @Test
    fun everySliderHasA48dpTouchTarget() {
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = ImageAdjust.Default,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = null,
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = true,
                    onAdjustChange = {},
                    onBack = {},
                )
            }
        }
        for (tag in listOf(
            IMAGE_ADJUST_BRIGHTNESS_TAG,
            IMAGE_ADJUST_CONTRAST_TAG,
            IMAGE_ADJUST_GAMMA_TAG,
            IMAGE_ADJUST_GRAYSCALE_TAG,
        )) {
            composeRule.onNodeWithTag(tag).performScrollTo().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun disabledGammaSliderDrawsNoActiveTrackColour() {
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = ImageAdjust.Default,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = null,
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = false,
                    onAdjustChange = {},
                    onBack = {},
                )
            }
        }
        val px = composeRule.onNodeWithTag(IMAGE_ADJUST_GAMMA_TAG).performScrollTo().captureToImage().toPixelMap()
        var live = 0
        for (x in 0 until px.width) {
            for (y in 0 until px.height) {
                val c = px[x, y]
                if (c.red * 255 > 200f && c.green * 255 < 120f && c.blue * 255 < 80f) live++
            }
        }
        assertEquals("primary pixels on a disabled slider", 0, live)
    }

    @Test
    fun resetHasItsTalkBackLabel() {
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = ImageAdjust.Default,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = null,
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = true,
                    onAdjustChange = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(IMAGE_ADJUST_RESET_TAG)
            .performScrollTo()
            .assertContentDescriptionEquals("보정 원래대로")
    }

    @Test
    fun resetStartsDisabledAtTheDefault() {
        composeRule.setContent {
            PdfBookTheme {
                ImageAdjustScreen(
                    adjust = ImageAdjust.Default,
                    bookTitle = "테스트 책",
                    page = 41,
                    renderer = null,
                    displayMode = DisplayMode.Default,
                    pageColorLabel = "기본",
                    gammaSupported = true,
                    onAdjustChange = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(IMAGE_ADJUST_RESET_TAG).assertIsNotEnabled()
    }
}
