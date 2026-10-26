package org.parkjw.apps.pdfbook.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReaderBrightnessSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository(): SettingsRepository {
        val file = File(tmp.root, "settings.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
        return SettingsRepository(store)
    }

    @Test
    fun defaultsFollowSystem() {
        val settings = AppSettings()
        assertTrue(settings.followSystemBrightness)
        assertEquals(100, settings.readerBrightnessPercent)
    }

    @Test
    fun manualLevelPersists() = runTest {
        val repo = repository()
        repo.update { it.copy(followSystemBrightness = false, readerBrightnessPercent = 35) }
        val loaded = repo.settings.first()
        assertFalse(loaded.followSystemBrightness)
        assertEquals(35, loaded.readerBrightnessPercent)
    }

    @Test
    fun outOfRangeLevelFallsBackToSystem() {
        val prefs = emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.FollowSystemBrightnessKey] = false
        prefs[SettingsRepository.ReaderBrightnessPercentKey] = 0
        val settings = prefs.toAppSettings()
        assertTrue(settings.followSystemBrightness)
        assertEquals(100, settings.readerBrightnessPercent)
    }

    @Test
    fun missingLevelFallsBackToSystem() {
        val prefs = emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.FollowSystemBrightnessKey] = false
        assertTrue(prefs.toAppSettings().followSystemBrightness)
    }

    @Test
    fun validLevelKeepsManualChoice() {
        val prefs = emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.FollowSystemBrightnessKey] = false
        prefs[SettingsRepository.ReaderBrightnessPercentKey] = 35
        val settings = prefs.toAppSettings()
        assertFalse(settings.followSystemBrightness)
        assertEquals(35, settings.readerBrightnessPercent)
    }
}
