package org.parkjw.apps.pdfbook.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.parkjw.apps.pdfbook.DEFAULT_READER_BRIGHTNESS_PERCENT
import org.parkjw.apps.pdfbook.isReaderBrightnessPercentValid
import org.parkjw.apps.pdfbook.normalizeReaderBrightnessPercent
import org.parkjw.apps.pdfbook.normalizeTapZoneSidePercent

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
        val ScreenOrientationKey = stringPreferencesKey("screen_orientation")
        val DoubleTapZoomScaleKey = floatPreferencesKey("double_tap_zoom_scale")
        val AutoUpdateCheckKey = booleanPreferencesKey("auto_update_check")
        val TapZoneSidePercentKey = intPreferencesKey("tap_zone_side_percent")
        val ShowDotFilesKey = booleanPreferencesKey("show_dot_files")
        val KeepZoomOnTurnKey = booleanPreferencesKey("keep_zoom_on_turn")
        val OneFingerZoomKey = booleanPreferencesKey("one_finger_zoom")
        val FollowSystemBrightnessKey = booleanPreferencesKey("follow_system_brightness")
        val ReaderBrightnessPercentKey = intPreferencesKey("reader_brightness_percent")
        val FullScreenReadingKey = booleanPreferencesKey("full_screen_reading")
        val DisplayModeKey = stringPreferencesKey("display_mode")
    }
}

internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    val brightnessPercent = this[SettingsRepository.ReaderBrightnessPercentKey]
    val brightnessValid = isReaderBrightnessPercentValid(brightnessPercent)
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
        screenOrientation = enumOrDefault(
            this[SettingsRepository.ScreenOrientationKey],
            defaults.screenOrientation,
        ),
        doubleTapZoomScale = this[SettingsRepository.DoubleTapZoomScaleKey]
            ?: defaults.doubleTapZoomScale,
        autoUpdateCheck = this[SettingsRepository.AutoUpdateCheckKey] ?: defaults.autoUpdateCheck,
        tapZoneSidePercent = normalizeTapZoneSidePercent(
            this[SettingsRepository.TapZoneSidePercentKey] ?: defaults.tapZoneSidePercent,
        ),
        showDotFiles = this[SettingsRepository.ShowDotFilesKey] ?: defaults.showDotFiles,
        keepZoomOnTurn = this[SettingsRepository.KeepZoomOnTurnKey] ?: defaults.keepZoomOnTurn,
        oneFingerZoom = this[SettingsRepository.OneFingerZoomKey] ?: defaults.oneFingerZoom,
        followSystemBrightness = if (brightnessValid) {
            this[SettingsRepository.FollowSystemBrightnessKey] ?: defaults.followSystemBrightness
        } else {
            defaults.followSystemBrightness
        },
        readerBrightnessPercent = if (brightnessValid) {
            brightnessPercent!!
        } else {
            DEFAULT_READER_BRIGHTNESS_PERCENT
        },
        fullScreenReading = this[SettingsRepository.FullScreenReadingKey] ?: defaults.fullScreenReading,
        displayMode = enumOrDefault(this[SettingsRepository.DisplayModeKey], defaults.displayMode),
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
    this[SettingsRepository.ScreenOrientationKey] = settings.screenOrientation.name
    this[SettingsRepository.DoubleTapZoomScaleKey] = settings.doubleTapZoomScale
    this[SettingsRepository.AutoUpdateCheckKey] = settings.autoUpdateCheck
    this[SettingsRepository.TapZoneSidePercentKey] = normalizeTapZoneSidePercent(settings.tapZoneSidePercent)
    this[SettingsRepository.ShowDotFilesKey] = settings.showDotFiles
    this[SettingsRepository.KeepZoomOnTurnKey] = settings.keepZoomOnTurn
    this[SettingsRepository.OneFingerZoomKey] = settings.oneFingerZoom
    this[SettingsRepository.FollowSystemBrightnessKey] = settings.followSystemBrightness
    this[SettingsRepository.ReaderBrightnessPercentKey] =
        normalizeReaderBrightnessPercent(settings.readerBrightnessPercent)
    this[SettingsRepository.FullScreenReadingKey] = settings.fullScreenReading
    this[SettingsRepository.DisplayModeKey] = settings.displayMode.name
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
