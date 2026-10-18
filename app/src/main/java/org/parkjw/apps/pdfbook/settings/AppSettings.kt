package org.parkjw.apps.pdfbook.settings

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
}

data class AppSettings(
    val startScreen: StartScreen = StartScreen.Folders,
    val themeMode: ThemeMode = ThemeMode.System,
    val volumeKeyPaging: Boolean = true,
    val defaultReadingDirection: ReadingDirection = ReadingDirection.RTL,
    val coverAlone: Boolean = true,
    val twoPageMode: TwoPageMode = TwoPageMode.Auto,
    val keepScreenOn: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
    val showTapZoneGuide: Boolean = true,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
    val doubleTapZoomScale: Float = 2f,
    val tapZoneSidePercent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
)
