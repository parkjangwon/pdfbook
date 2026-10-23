package org.parkjw.apps.pdfbook.settings

import android.content.pm.ActivityInfo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScreenOrientationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun mapsToActivityOrientation() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, ScreenOrientation.Auto.toActivityOrientation())
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT,
            ScreenOrientation.Portrait.toActivityOrientation(),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,
            ScreenOrientation.Landscape.toActivityOrientation(),
        )
    }

    @Test
    fun defaultIsAuto() {
        assertEquals(ScreenOrientation.Auto, AppSettings().screenOrientation)
    }

    @Test
    fun unknownStoredValueFallsBackToAuto() {
        val prefs = androidx.datastore.preferences.core.emptyPreferences().toMutablePreferences()
        prefs[SettingsRepository.ScreenOrientationKey] = "Sideways"
        assertEquals(ScreenOrientation.Auto, prefs.toAppSettings().screenOrientation)
    }

    @Test
    fun persistsAndReplays() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val repo = repository(file)
        assertEquals(ScreenOrientation.Auto, repo.settings.first().screenOrientation)
        repo.update { it.copy(screenOrientation = ScreenOrientation.Landscape) }
        assertEquals(ScreenOrientation.Landscape, repo.settings.first().screenOrientation)
    }

    private fun TestScope.repository(file: File): SettingsRepository {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
        return SettingsRepository(store)
    }
}
