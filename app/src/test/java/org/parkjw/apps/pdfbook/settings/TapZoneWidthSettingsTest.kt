package org.parkjw.apps.pdfbook.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TapZoneWidthSettingsTest {
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
    fun defaultWidthIsThirtyThree() = runTest {
        assertEquals(33, AppSettings().tapZoneSidePercent)
        assertEquals(33, repository().settings.first().tapZoneSidePercent)
    }

    @Test
    fun widthSurvivesRestart() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val firstJob = SupervisorJob(coroutineContext.job)
        val first = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(coroutineContext + firstJob),
                produceFile = { file },
            ),
        )
        first.update { it.copy(tapZoneSidePercent = 40) }
        firstJob.cancelAndJoin()
        val second = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file },
            ),
        )
        assertEquals(40, second.settings.first().tapZoneSidePercent)
    }

    @Test
    fun writesAreNormalized() = runTest {
        val repo = repository()
        repo.update { it.copy(tapZoneSidePercent = 99) }
        assertEquals(45, repo.settings.first().tapZoneSidePercent)
        repo.update { it.copy(tapZoneSidePercent = 27) }
        assertEquals(25, repo.settings.first().tapZoneSidePercent)
    }

    @Test
    fun storedOutOfRangeValueIsNormalizedOnRead() {
        val prefs = emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.TapZoneSidePercentKey] = 7
        assertEquals(20, prefs.toAppSettings().tapZoneSidePercent)
        prefs[SettingsRepository.TapZoneSidePercentKey] = 33
        assertEquals(33, prefs.toAppSettings().tapZoneSidePercent)
    }
}
