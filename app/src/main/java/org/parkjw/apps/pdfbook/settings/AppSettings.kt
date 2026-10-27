package org.parkjw.apps.pdfbook.settings

import org.parkjw.apps.pdfbook.DEFAULT_READER_BRIGHTNESS_PERCENT
import android.content.pm.ActivityInfo
import org.parkjw.apps.pdfbook.DEFAULT_TAP_ZONE_SIDE_PERCENT
import org.parkjw.apps.pdfbook.ReadingDirection

enum class StartScreen {
    Folders,
    LastBook,
}

enum class ThemeMode {
    System,
    Dark,
    Light,
}

enum class TwoPageMode {
    Auto,
    AlwaysSingle,
    AlwaysDouble,
}

enum class AppLanguage {
    System,
    Korean,
    English,
}

enum class PageTurnAnimation {
    Slide,
    None,
    Curl,
}

enum class ScreenOrientation {
    Auto,
    Portrait,
    Landscape,
}

fun ScreenOrientation.toActivityOrientation(): Int = when (this) {
    ScreenOrientation.Auto -> ActivityInfo.SCREEN_ORIENTATION_USER
    ScreenOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    ScreenOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
}

// Persisted by name in SettingsRepository; renaming an entry resets users to Default.
enum class DisplayMode {
    Default,
    Invert,
    Sepia,
}

data class AppSettings(
    val startScreen: StartScreen = StartScreen.Folders,
    val themeMode: ThemeMode = ThemeMode.System,
    val volumeKeyPaging: Boolean = true,
    val defaultReadingDirection: ReadingDirection = ReadingDirection.RTL,
    val coverAlone: Boolean = true,
    val widePageAlone: Boolean = false,
    val twoPageMode: TwoPageMode = TwoPageMode.Auto,
    val keepScreenOn: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
    val showTapZoneGuide: Boolean = true,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    val doubleTapZoomScale: Float = 2f,
    val autoUpdateCheck: Boolean = true,
    val tapZoneSidePercent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
    val showDotFiles: Boolean = false,
    val keepZoomOnTurn: Boolean = false,
    val oneFingerZoom: Boolean = false,
    val followSystemBrightness: Boolean = true,
    val readerBrightnessPercent: Int = DEFAULT_READER_BRIGHTNESS_PERCENT,
    val fullScreenReading: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.Default,
)
