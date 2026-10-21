package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.settings.TwoPageMode

class TwoPageTest {
    private val fold = IntRect(400, 0, 420, 800)

    @Test
    fun flatAutoUsesWidth() {
        assertTrue(shouldUseTwoPage(TwoPageMode.Auto, 600, FoldPosture.Flat))
        assertTrue(shouldUseTwoPage(TwoPageMode.Auto, 800, FoldPosture.Flat))
        assertFalse(shouldUseTwoPage(TwoPageMode.Auto, 599, FoldPosture.Flat))
        assertFalse(shouldUseTwoPage(TwoPageMode.Auto, 360, FoldPosture.Flat))
    }

    @Test
    fun flatAlwaysModes() {
        assertFalse(shouldUseTwoPage(TwoPageMode.AlwaysSingle, 800, FoldPosture.Flat))
        assertTrue(shouldUseTwoPage(TwoPageMode.AlwaysDouble, 360, FoldPosture.Flat))
    }

    @Test
    fun tabletopAlwaysSingle() {
        val tabletop = FoldPosture.Tabletop(fold)
        assertFalse(shouldUseTwoPage(TwoPageMode.Auto, 800, tabletop))
        assertFalse(shouldUseTwoPage(TwoPageMode.AlwaysDouble, 800, tabletop))
        assertFalse(shouldUseTwoPage(TwoPageMode.AlwaysSingle, 800, tabletop))
    }

    @Test
    fun bookRespectsMode() {
        val book = FoldPosture.Book(fold)
        assertTrue(shouldUseTwoPage(TwoPageMode.Auto, 360, book))
        assertTrue(shouldUseTwoPage(TwoPageMode.AlwaysDouble, 360, book))
        assertFalse(shouldUseTwoPage(TwoPageMode.AlwaysSingle, 800, book))
    }
}
