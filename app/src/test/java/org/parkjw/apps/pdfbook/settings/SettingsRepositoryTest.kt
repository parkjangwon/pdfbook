package org.parkjw.apps.pdfbook.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository(fileName: String = "settings.preferences_pb"): SettingsRepository {
        val file = File(tmp.root, fileName)
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
        return SettingsRepository(store)
    }

    @Test
    fun emptyStoreYieldsDefaults() = runTest {
        assertEquals(AppSettings(), repository().settings.first())
    }

    @Test
    fun updatePersistsAndReplays() = runTest {
        val repo = repository()
        repo.update {
            it.copy(
                themeMode = ThemeMode.Dark,
                volumeKeyPaging = false,
                twoPageMode = TwoPageMode.AlwaysDouble,
                startScreen = StartScreen.LastBook,
                doubleTapZoomScale = 2f,
            )
        }
        val loaded = repo.settings.first()
        assertEquals(ThemeMode.Dark, loaded.themeMode)
        assertFalse(loaded.volumeKeyPaging)
        assertEquals(TwoPageMode.AlwaysDouble, loaded.twoPageMode)
        assertEquals(StartScreen.LastBook, loaded.startScreen)
        assertEquals(2f, loaded.doubleTapZoomScale, 0f)
    }

    @Test
    fun readingInfoDefaultsOffAndRoundTrips() = runTest {
        val repo = repository()
        val defaults = repo.settings.first()
        assertFalse(defaults.readingInfoPage)
        assertFalse(defaults.readingInfoTime)
        assertFalse(defaults.readingInfoBattery)
        repo.update { it.copy(readingInfoPage = true, readingInfoBattery = true) }
        val loaded = repo.settings.first()
        assertEquals(true, loaded.readingInfoPage)
        assertFalse(loaded.readingInfoTime)
        assertEquals(true, loaded.readingInfoBattery)
    }

    @Test
    fun corruptEnumFallsBackToDefault() {
        val prefs = androidx.datastore.preferences.core.emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.ThemeModeKey] = "NotATheme"
        assertEquals(ThemeMode.System, prefs.toAppSettings().themeMode)
    }

    @Test
    fun legacyLibraryStartScreenMigratesToFolders() {
        assertEquals(StartScreen.Folders, startScreenOrDefault("Library", StartScreen.LastBook))
        assertEquals(StartScreen.Folders, startScreenOrDefault("Folders", StartScreen.LastBook))
        assertEquals(StartScreen.LastBook, startScreenOrDefault("LastBook", StartScreen.Folders))
        assertEquals(StartScreen.Folders, startScreenOrDefault("Nope", StartScreen.Folders))
    }
}
