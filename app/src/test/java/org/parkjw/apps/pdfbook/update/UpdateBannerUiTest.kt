package org.parkjw.apps.pdfbook.update

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko")
class UpdateBannerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bannerShowsVersionAndBothActions() {
        val clicks = mutableListOf<String>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                UpdateBanner(
                    version = "v0.3.1",
                    onLater = { clicks += "later" },
                    onDownload = { clicks += "download" },
                )
            }
        }
        composeRule.onNodeWithText("새 버전이 있어요 (v0.3.1)").assertIsDisplayed()
        composeRule.onNodeWithText("나중에").performClick()
        composeRule.onNodeWithText("받고 설치하기").performClick()
        assertEquals(listOf("later", "download"), clicks)
    }
}
