package org.parkjw.apps.pdfbook.hidden

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import java.io.File

class HiddenPeekTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun resetPeek() {
        HiddenPeek.reset()
    }

    @Test
    fun h10_peekIsMemoryOnlyAndResets() = runBlocking {
        val hiddenFile = File(tmp.root, "hidden.preferences_pb")
        val settingsFile = File(tmp.root, "settings.preferences_pb")
        val hidden = HiddenRepository(PreferenceDataStoreFactory.create { hiddenFile })
        val settings = SettingsRepository(PreferenceDataStoreFactory.create { settingsFile })
        hidden.hide(HiddenItem(HiddenKind.Book, "content://b", "Book", null))
        settings.update { AppSettings(showDotFiles = true) }
        settings.settings.first()
        val beforeHidden = hiddenFile.readBytes()
        val beforeSettings = settingsFile.readBytes()
        HiddenPeek.set(true)
        assertTrue(HiddenPeek.enabled.value)
        assertTrue(hiddenFile.readBytes().contentEquals(beforeHidden))
        assertTrue(settingsFile.readBytes().contentEquals(beforeSettings))
        HiddenPeek.reset()
        assertFalse(HiddenPeek.enabled.value)
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() || File(it, "settings.gradle").exists() }
        val text = File(root, "app/src/main/java/org/parkjw/apps/pdfbook/hidden/HiddenPeek.kt").readText()
        assertFalse(text.contains("DataStore"))
        assertFalse(text.contains("rememberSaveable"))
        assertFalse(text.contains("SharedPreferences"))
    }

    @Test
    fun h11_onStopStyleResetTurnsPeekOff() {
        HiddenPeek.set(true)
        HiddenPeek.reset()
        assertFalse(HiddenPeek.enabled.value)
    }
}
