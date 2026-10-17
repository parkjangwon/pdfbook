package org.parkjw.apps.pdfbook.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun repository(): SettingsRepository {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        return SettingsRepository(store)
    }

    @Test
    fun emptyStoreYieldsDefaults() = runBlocking {
        assertEquals(AppSettings(), repository().settings.first())
    }

    @Test
    fun updatePersistsAndReplays() = runBlocking {
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
    fun corruptEnumFallsBackToDefault() {
        val prefs = androidx.datastore.preferences.core.emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.ThemeModeKey] = "NotATheme"
        assertEquals(ThemeMode.System, prefs.toAppSettings().themeMode)
    }
}
