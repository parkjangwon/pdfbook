package org.parkjw.apps.pdfbook.hidden

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.MainActivity
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class HiddenPeekTest {
    @Before
    fun resetPeek() {
        HiddenPeek.reset()
    }

    @Test
    fun h10_peekIsMemoryOnlyAndResets() {
        assertFalse(HiddenPeek.enabled.value)
        HiddenPeek.set(true)
        assertTrue(HiddenPeek.enabled.value)
        HiddenPeek.reset()
        assertFalse(HiddenPeek.enabled.value)
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() || File(it, "settings.gradle").exists() }
        val peekText = File(root, "app/src/main/java/org/parkjw/apps/pdfbook/hidden/HiddenPeek.kt").readText()
        val mainLines = File(root, "app/src/main/java/org/parkjw/apps/pdfbook/MainActivity.kt").readLines()
            .filter { line ->
                line.contains("HiddenPeek") || line.contains("shouldResetPeekOnStop")
            }
        val forbidden = listOf(
            "DataStore",
            "SharedPreferences",
            "rememberSaveable",
            "savedInstanceState",
            "onSaveInstanceState",
        )
        for (token in forbidden) {
            assertFalse(peekText.contains(token))
            assertTrue(mainLines.none { it.contains(token) })
        }
    }

    @Test
    fun h11_onStopResetsPeekButRecreateKeepsIt() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            HiddenPeek.set(true)
            scenario.recreate()
            assertTrue(HiddenPeek.enabled.value)
            scenario.moveToState(Lifecycle.State.CREATED)
            assertFalse(HiddenPeek.enabled.value)
        }
    }

    @Test
    fun h11_shouldResetPeekOnStopOnlyWhenLeaving() {
        assertTrue(shouldResetPeekOnStop(false))
        assertFalse(shouldResetPeekOnStop(true))
    }
}
