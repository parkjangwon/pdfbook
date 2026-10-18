package org.parkjw.apps.pdfbook.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TapZoneWidthSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun defaultWidthIsThirtyThree() = runBlocking {
        assertEquals(33, AppSettings().tapZoneSidePercent)
        withRepository { repo ->
            assertEquals(33, repo.settings.first().tapZoneSidePercent)
        }
    }

    @Test
    fun widthSurvivesRestart() = runBlocking {
        withRepository { repo -> repo.update { it.copy(tapZoneSidePercent = 40) } }
        withRepository { repo ->
            assertEquals(40, repo.settings.first().tapZoneSidePercent)
        }
    }

    @Test
    fun writesAreNormalized() = runBlocking {
        withRepository { repo ->
            repo.update { it.copy(tapZoneSidePercent = 99) }
            assertEquals(45, repo.settings.first().tapZoneSidePercent)
            repo.update { it.copy(tapZoneSidePercent = 27) }
            assertEquals(25, repo.settings.first().tapZoneSidePercent)
        }
    }

    @Test
    fun storedOutOfRangeValueIsNormalizedOnRead() {
        val prefs = emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.TapZoneSidePercentKey] = 7
        assertEquals(20, prefs.toAppSettings().tapZoneSidePercent)
        prefs[SettingsRepository.TapZoneSidePercentKey] = 33
        assertEquals(33, prefs.toAppSettings().tapZoneSidePercent)
    }

    private suspend fun withRepository(block: suspend (SettingsRepository) -> Unit) {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) {
            File(tmp.root, "settings.preferences_pb")
        }
        try {
            block(SettingsRepository(store))
        } finally {
            job.cancelAndJoin()
        }
    }
}
