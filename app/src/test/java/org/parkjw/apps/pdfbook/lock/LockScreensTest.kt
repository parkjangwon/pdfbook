package org.parkjw.apps.pdfbook.lock

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
class LockScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

    private fun tapDigit(digit: Char) {
        composeRule.onNode(hasText(digit.toString()) and hasClickAction()).performClick()
    }

    @Test
    fun l1_noneModeShowsAppImmediately() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockGate(
                    config = LockConfig(),
                    session = AppLockState(),
                    canAuthenticate = false,
                    verify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onUnlocked = {},
                    onBack = {},
                    onBlockedChange = {},
                ) {
                    Text("APP")
                }
            }
        }
        composeRule.onNodeWithText("APP").assertExists()
    }

    @Test
    fun l2_appStaysUncomposedUntilUnlockAndSaveableReturns() {
        val session = AppLockState()
        session.unlock()
        val config = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockGate(
                    config = config,
                    session = session,
                    canAuthenticate = false,
                    verify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onUnlocked = { session.unlock() },
                    onBack = {},
                    onBlockedChange = {},
                ) {
                    var uri by rememberSaveable { mutableStateOf("book-uri") }
                    Text(uri)
                }
            }
        }
        composeRule.onNodeWithText("book-uri").assertExists()
        session.coldLock()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("book-uri").assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.lock_title)).assertExists()
        session.unlock()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("book-uri").assertExists()
    }

    @Test
    fun l5_biometricWithoutPinIsDisabledAndShowsNeedsPin() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                AppLockSettingsScreen(
                    config = LockConfig(),
                    session = AppLockState(),
                    biometricAvailable = true,
                    onBack = {},
                    onAuthenticated = {},
                    onVerify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onSavePin = { pin, _ -> pin.fill('\u0000') },
                    onSetMode = { false },
                    onSetHideInRecents = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.app_lock_biometric)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.app_lock_biometric_needs_pin)).assertExists()
        composeRule.onNodeWithText(text(R.string.pin_create_title)).assertDoesNotExist()
    }

    @Test
    fun l5_noneModeHidesPinChangeAndHideInRecents() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                AppLockSettingsMenu(
                    config = LockConfig(),
                    biometricAvailable = true,
                    onBack = {},
                    onEdit = {},
                    onHideChange = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.pin_change)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.hide_in_recents)).assertDoesNotExist()
    }

    @Test
    fun l7_lockScreenShowsWaitAndDisablesKeys() {
        assertEquals(30_000L, lockoutMillisFor(5))
        val now = 1_000_000L
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockScreen(
                    config = LockConfig(
                        mode = AppLockMode.Pin,
                        pinHash = "h",
                        pinSalt = "s",
                        pinLength = 4,
                        failedAttempts = 5,
                        lockoutUntilEpochMs = now + 30_000L,
                    ),
                    session = AppLockState(),
                    canAuthenticate = false,
                    verify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onUnlocked = {},
                    onBack = {},
                    now = { now },
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.lock_wait, 30)).assertExists()
        composeRule.onNode(hasText("1") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun l8_turningLockOffAsksBeforeChangingMode() {
        var changed = false
        val config = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                AppLockSettingsScreen(
                    config = config,
                    session = AppLockState(),
                    biometricAvailable = false,
                    onBack = {},
                    onAuthenticated = {},
                    onVerify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onSavePin = { pin, _ -> pin.fill('\u0000') },
                    onSetMode = {
                        changed = true
                        true
                    },
                    onSetHideInRecents = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.app_lock_none)).performClick()
        composeRule.onNodeWithText(text(R.string.confirm_identity)).assertExists()
        assertFalse(changed)
    }

    @Test
    fun l4_keypadVisibleAfterUsePin() {
        val state = onLockUsePin(initialLockInteraction(AppLockMode.Biometric, canAuthenticate = true, preferKeypad = false))
        assertTrue(state.keypad)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ConfirmPinScreen(
                    config = LockConfig(mode = AppLockMode.Biometric, pinHash = "h", pinSalt = "s", pinLength = 4),
                    session = AppLockState(),
                    canAuthenticate = true,
                    forceKeypad = state.keypad,
                    verify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onSuccess = {},
                    onUsePin = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.confirm_identity)).assertExists()
        composeRule.onNodeWithText("0").assertExists()
    }

    @Test
    fun l10_lockScreenRendersOnlyLockCopy() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockScreen(
                    config = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4),
                    session = AppLockState(),
                    canAuthenticate = false,
                    verify = { pin, _ -> pin.fill('\u0000'); PinVerify.Wrong },
                    onUnlocked = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.lock_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.lock_enter_pin)).assertExists()
        composeRule.onNodeWithText("Sample Folder").assertDoesNotExist()
        composeRule.onNodeWithText("샘플 책").assertDoesNotExist()
    }

    @Test
    fun l11_pinCreateShowsForgetWarning() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                PinCreateScreen(onCreated = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText(text(R.string.pin_forget_warning)).assertExists()
        assertEquals(
            "PIN을 잊으면 되찾을 수 없어요. 앱 데이터를 지워야 하고, 폴더 목록과 읽던 기록도 함께 지워져요.",
            text(R.string.pin_forget_warning),
        )
    }

    @Test
    fun l12_lockDotsGrowWithInputAndAutoVerify() {
        val verified = AtomicReference<String?>(null)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LockScreen(
                    config = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 6),
                    session = AppLockState(),
                    canAuthenticate = false,
                    verify = { pin, _ ->
                        verified.set(String(pin))
                        pin.fill('\u0000')
                        PinVerify.Wrong
                    },
                    onUnlocked = {},
                    onBack = {},
                )
            }
        }
        composeRule.onAllNodesWithTag(PIN_DOT_TAG).assertCountEquals(0)
        tapDigit('1')
        tapDigit('2')
        composeRule.onAllNodesWithTag(PIN_DOT_TAG).assertCountEquals(2)
        composeRule.onNodeWithContentDescription(text(R.string.app_lock_biometric)).assertDoesNotExist()
        "3456".forEach { tapDigit(it) }
        composeRule.waitForIdle()
        assertEquals("123456", verified.get())
    }

    @Test
    fun l13_pinCreateNextGateCapAndConfirm() {
        val created = AtomicReference<String?>(null)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                PinCreateScreen(
                    onCreated = { pin ->
                        created.set(String(pin))
                        pin.fill('\u0000')
                    },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("pin_next").assertIsNotEnabled()
        "123".forEach { tapDigit(it) }
        composeRule.onNodeWithTag("pin_next").assertIsNotEnabled()
        tapDigit('4')
        composeRule.onNodeWithTag("pin_next").assertIsEnabled()
        "5678".forEach { tapDigit(it) }
        tapDigit('9')
        composeRule.onAllNodesWithTag(PIN_DOT_TAG).assertCountEquals(8)
        composeRule.onNodeWithTag("pin_next").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.pin_confirm_title)).assertExists()
        "12345679".forEach { tapDigit(it) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.pin_mismatch)).assertExists()
        composeRule.onNodeWithText(text(R.string.pin_create_title)).assertExists()
        "1357".forEach { tapDigit(it) }
        composeRule.onNodeWithTag("pin_next").performScrollTo().performClick()
        composeRule.waitForIdle()
        "1357".forEach { tapDigit(it) }
        composeRule.waitForIdle()
        assertEquals("1357", created.get())
    }
}
