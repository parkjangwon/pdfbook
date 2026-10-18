package org.parkjw.apps.pdfbook.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.settingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            prefs.write(transform(prefs.toAppSettings()))
        }
    }

    companion object {
        val StartScreenKey = stringPreferencesKey("start_screen")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val VolumeKeyPagingKey = booleanPreferencesKey("volume_key_paging")
        val DefaultReadingDirectionKey = stringPreferencesKey("default_reading_direction")
        val CoverAloneKey = booleanPreferencesKey("cover_alone")
        val TwoPageModeKey = stringPreferencesKey("two_page_mode")
        val KeepScreenOnKey = booleanPreferencesKey("keep_screen_on")
        val LanguageKey = stringPreferencesKey("language")
        val ShowTapZoneGuideKey = booleanPreferencesKey("show_tap_zone_guide")
        val PageTurnAnimationKey = stringPreferencesKey("page_turn_animation")
        val DoubleTapZoomScaleKey = floatPreferencesKey("double_tap_zoom_scale")
        val ShowDotFilesKey = booleanPreferencesKey("show_dot_files")
    }
}

internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        startScreen = startScreenOrDefault(this[SettingsRepository.StartScreenKey], defaults.startScreen),
        themeMode = enumOrDefault(this[SettingsRepository.ThemeModeKey], defaults.themeMode),
        volumeKeyPaging = this[SettingsRepository.VolumeKeyPagingKey] ?: defaults.volumeKeyPaging,
        defaultReadingDirection = enumOrDefault(
            this[SettingsRepository.DefaultReadingDirectionKey],
            defaults.defaultReadingDirection,
        ),
        coverAlone = this[SettingsRepository.CoverAloneKey] ?: defaults.coverAlone,
        twoPageMode = enumOrDefault(this[SettingsRepository.TwoPageModeKey], defaults.twoPageMode),
        keepScreenOn = this[SettingsRepository.KeepScreenOnKey] ?: defaults.keepScreenOn,
        language = enumOrDefault(this[SettingsRepository.LanguageKey], defaults.language),
        showTapZoneGuide = this[SettingsRepository.ShowTapZoneGuideKey] ?: defaults.showTapZoneGuide,
        pageTurnAnimation = enumOrDefault(
            this[SettingsRepository.PageTurnAnimationKey],
            defaults.pageTurnAnimation,
        ),
        doubleTapZoomScale = this[SettingsRepository.DoubleTapZoomScaleKey]
            ?: defaults.doubleTapZoomScale,
        showDotFiles = this[SettingsRepository.ShowDotFilesKey] ?: defaults.showDotFiles,
    )
}

private fun MutablePreferences.write(settings: AppSettings) {
    this[SettingsRepository.StartScreenKey] = settings.startScreen.name
    this[SettingsRepository.ThemeModeKey] = settings.themeMode.name
    this[SettingsRepository.VolumeKeyPagingKey] = settings.volumeKeyPaging
    this[SettingsRepository.DefaultReadingDirectionKey] = settings.defaultReadingDirection.name
    this[SettingsRepository.CoverAloneKey] = settings.coverAlone
    this[SettingsRepository.TwoPageModeKey] = settings.twoPageMode.name
    this[SettingsRepository.KeepScreenOnKey] = settings.keepScreenOn
    this[SettingsRepository.LanguageKey] = settings.language.name
    this[SettingsRepository.ShowTapZoneGuideKey] = settings.showTapZoneGuide
    this[SettingsRepository.PageTurnAnimationKey] = settings.pageTurnAnimation.name
    this[SettingsRepository.DoubleTapZoomScaleKey] = settings.doubleTapZoomScale
    this[SettingsRepository.ShowDotFilesKey] = settings.showDotFiles
}

private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw == null) return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}

internal fun startScreenOrDefault(raw: String?, default: StartScreen): StartScreen =
    when (raw) {
        null -> default
        "Library", StartScreen.Folders.name -> StartScreen.Folders
        StartScreen.LastBook.name -> StartScreen.LastBook
        else -> default
    }
