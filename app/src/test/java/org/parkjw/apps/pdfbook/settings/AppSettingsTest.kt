package org.parkjw.apps.pdfbook.settings

import org.parkjw.apps.pdfbook.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultsMatchScope() {
        val s = AppSettings()
        assertEquals(StartScreen.Folders, s.startScreen)
        assertEquals(ThemeMode.System, s.themeMode)
        assertTrue(s.volumeKeyPaging)
        assertEquals(ReadingDirection.RTL, s.defaultReadingDirection)
        assertTrue(s.coverAlone)
        assertEquals(TwoPageMode.Auto, s.twoPageMode)
        assertTrue(s.keepScreenOn)
        assertEquals(AppLanguage.System, s.language)
        assertTrue(s.showTapZoneGuide)
        assertEquals(PageTurnAnimation.Slide, s.pageTurnAnimation)
        assertEquals(2f, s.doubleTapZoomScale, 0f)
        assertEquals(false, s.showDotFiles)
        assertEquals(15, s.autoTurnSeconds)
        assertTrue(s.followSystemBrightness)
        assertEquals(100, s.readerBrightnessPercent)
    }
}
