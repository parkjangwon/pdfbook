package org.parkjw.apps.pdfbook

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.pm.ShortcutManagerCompat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.lock.AppLockMode
import org.parkjw.apps.pdfbook.lock.AppLockProcess
import org.parkjw.apps.pdfbook.lock.LockRepository
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class ContinueShortcutTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun shortcutExistsOnlyWhileThereIsALastBook() {
        syncContinueShortcut(context, hasLastBook = true)
        val shortcut = ShortcutManagerCompat.getDynamicShortcuts(context).single()
        assertEquals(ACTION_CONTINUE_READING, shortcut.intent.action)
        assertEquals(context.getString(R.string.shortcut_continue_reading), shortcut.shortLabel.toString())
        syncContinueShortcut(context, hasLastBook = false)
        assertTrue(ShortcutManagerCompat.getDynamicShortcuts(context).isEmpty())
    }

    @Test
    fun shortcutLaunchStillShowsLockFirst() {
        AppLockProcess.state.resetForTest()
        runBlocking { LockRepository(context).saveNewPin("1234".toCharArray(), AppLockMode.Pin) }
        val intent = Intent(context, MainActivity::class.java).setAction(ACTION_CONTINUE_READING)
        assertTrue(intent.isContinueReading())
        ActivityScenario.launch<MainActivity>(intent).use {
            // DataStore loads on an IO thread, so pump the main looper while waiting for the pad.
            val deadline = System.currentTimeMillis() + 5_000
            while (composeRule.onAllNodesWithText("1").fetchSemanticsNodes().isEmpty() &&
                System.currentTimeMillis() < deadline
            ) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(50)
            }
            assertTrue(composeRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty())
            assertFalse(AppLockProcess.state.unlocked)
            composeRule.onAllNodesWithTag("reader_screen").fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        }
    }
}
