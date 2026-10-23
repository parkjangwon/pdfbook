package org.parkjw.apps.pdfbook.lock

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko")
class PinEntryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun tap(s: String) = s.forEach {
        composeRule.onNode(hasText(it.toString()) and hasClickAction()).performClick()
    }

    private val pinConfig = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4)

    private fun screen(verify: suspend (CharArray, Long) -> PinVerify, onUnlocked: () -> Unit = {}) =
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockScreen(pinConfig, AppLockState(), false, verify, onUnlocked, {})
            }
        }

    @Test
    fun correctPinCallsOnUnlockedOnce() {
        val unlocked = AtomicInteger()
        screen({ pin, _ -> pin.fill('\u0000'); PinVerify.Success }) { unlocked.incrementAndGet() }
        tap("1234")
        composeRule.waitForIdle()
        assertEquals(1, unlocked.get())
    }

    @Test
    fun wrongPinShowsErrorClearsDotsAndNextDigitClearsError() {
        screen({ pin, _ -> pin.fill('\u0000'); PinVerify.Wrong })
        tap("1234")
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.lock_wrong_pin)).assertExists()
        composeRule.onAllNodesWithTag(PIN_DOT_TAG).assertCountEquals(0)
        tap("1")
        composeRule.onNodeWithText(text(R.string.lock_wrong_pin)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.lock_enter_pin)).assertExists()
    }

    @Test
    fun shorterThanPinLengthNeverVerifies() {
        val calls = AtomicInteger()
        screen({ pin, _ -> calls.incrementAndGet(); pin.fill('\u0000'); PinVerify.Wrong })
        tap("123")
        composeRule.waitForIdle()
        assertEquals(0, calls.get())
        composeRule.onAllNodesWithTag(PIN_DOT_TAG).assertCountEquals(3)
    }
}
