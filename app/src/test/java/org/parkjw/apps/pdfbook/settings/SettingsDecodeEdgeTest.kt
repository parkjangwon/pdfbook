package org.parkjw.apps.pdfbook.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.ReadingDirection

class SettingsDecodeEdgeTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.store(fileName: String = "settings.preferences_pb") =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { File(tmp.root, fileName) })

    private class FailingStore(private val error: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw error }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            throw error
    }

    @Test
    fun everyUnknownEnumValueFallsBackToItsDefault() {
        val prefs = emptyPreferences().toMutablePreferences().apply {
            this[SettingsRepository.StartScreenKey] = "Home"
            this[SettingsRepository.ThemeModeKey] = "dark"
            this[SettingsRepository.DefaultReadingDirectionKey] = "UP"
            this[SettingsRepository.TwoPageModeKey] = ""
            this[SettingsRepository.LanguageKey] = "Japanese"
            this[SettingsRepository.PageTurnAnimationKey] = "Curl"
        }
        assertEquals(AppSettings(), prefs.toAppSettings())
    }

    @Test
    fun enumNamesAreCaseSensitiveAndNotTrimmed() {
        val prefs = emptyPreferences().toMutablePreferences().apply {
            this[SettingsRepository.ThemeModeKey] = "DARK"
            this[SettingsRepository.LanguageKey] = " Korean"
            this[SettingsRepository.StartScreenKey] = "folders"
        }
        val s = prefs.toAppSettings()
        assertEquals(ThemeMode.System, s.themeMode)
        assertEquals(AppLanguage.System, s.language)
        assertEquals(StartScreen.Folders, s.startScreen)
    }

    @Test
    fun startScreenNullKeepsGivenDefault() {
        assertEquals(StartScreen.LastBook, startScreenOrDefault(null, StartScreen.LastBook))
        assertEquals(StartScreen.LastBook, startScreenOrDefault("library", StartScreen.LastBook))
    }

    @Test
    fun storedTapZonePercentIsNormalizedOnRead() {
        fun read(value: Int) = emptyPreferences().toMutablePreferences()
            .apply { this[SettingsRepository.TapZoneSidePercentKey] = value }
            .toAppSettings().tapZoneSidePercent
        assertEquals(20, read(Int.MIN_VALUE))
        assertEquals(20, read(-1))
        assertEquals(45, read(99))
        assertEquals(45, read(Int.MAX_VALUE))
        assertEquals(33, read(33))
    }

    @Test
    fun partialStoreMixesStoredValuesWithDefaults() {
        val prefs = emptyPreferences().toMutablePreferences().apply {
            this[SettingsRepository.KeepScreenOnKey] = false
            this[SettingsRepository.DefaultReadingDirectionKey] = "LTR"
        }
        assertEquals(
            AppSettings(keepScreenOn = false, defaultReadingDirection = ReadingDirection.LTR),
            prefs.toAppSettings(),
        )
    }

    @Test
    fun everyFieldRoundTripsThroughUpdateAndNewRepository() = runTest {
        val data = store()
        val changed = AppSettings(
            startScreen = StartScreen.LastBook,
            themeMode = ThemeMode.Light,
            volumeKeyPaging = false,
            defaultReadingDirection = ReadingDirection.LTR,
            coverAlone = false,
            twoPageMode = TwoPageMode.AlwaysSingle,
            keepScreenOn = false,
            language = AppLanguage.Korean,
            showTapZoneGuide = false,
            pageTurnAnimation = PageTurnAnimation.None,
            doubleTapZoomScale = 3.5f,
            autoUpdateCheck = false,
            tapZoneSidePercent = 40,
            showDotFiles = true,
            keepZoomOnTurn = true,
        )
        SettingsRepository(data).update { changed }
        assertEquals(changed, SettingsRepository(data).settings.first())
    }

    @Test
    fun updateTransformSeesDefaultsOnEmptyStore() = runTest {
        var seen: AppSettings? = null
        SettingsRepository(store()).update { seen = it; it }
        assertEquals(AppSettings(), seen)
    }

    @Test
    fun updateRepairsCorruptStoredValuesAndNormalizesTapZone() = runTest {
        val data = store()
        data.edit {
            it[SettingsRepository.ThemeModeKey] = "Neon"
            it[SettingsRepository.TapZoneSidePercentKey] = 200
        }
        val repo = SettingsRepository(data)
        repo.update { it.copy(volumeKeyPaging = false) }

        val prefs = data.data.first()
        assertEquals("System", prefs[SettingsRepository.ThemeModeKey])
        assertEquals(45, prefs[SettingsRepository.TapZoneSidePercentKey])
        assertFalse(repo.settings.first().volumeKeyPaging)
    }

    @Test
    fun updateWithOutOfRangeTapZoneStoresNormalizedValue() = runTest {
        val data = store()
        SettingsRepository(data).update { it.copy(tapZoneSidePercent = 3) }
        assertEquals(20, data.data.first()[SettingsRepository.TapZoneSidePercentKey])
    }

    @Test
    fun consecutiveUpdatesComposeOnLatestState() = runTest {
        val repo = SettingsRepository(store())
        repo.update { it.copy(showDotFiles = true) }
        repo.update { it.copy(keepZoomOnTurn = true) }
        val s = repo.settings.first()
        assertTrue(s.showDotFiles)
        assertTrue(s.keepZoomOnTurn)
    }

    @Test
    fun ioErrorWhileReadingYieldsDefaults() = runTest {
        val repo = SettingsRepository(FailingStore(IOException("corrupt")))
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test
    fun nonIoErrorWhileReadingPropagates() = runTest {
        val repo = SettingsRepository(FailingStore(IllegalStateException("boom")))
        try {
            repo.settings.first()
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
    }
}
