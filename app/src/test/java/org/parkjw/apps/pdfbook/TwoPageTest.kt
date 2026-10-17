package org.parkjw.apps.pdfbook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwoPageTest {
    @Test
    fun wideEnoughUsesTwoPage() {
        assertTrue(shouldUseTwoPage(600, halfOpened = false))
        assertTrue(shouldUseTwoPage(800, halfOpened = false))
    }

    @Test
    fun narrowUsesOnePage() {
        assertFalse(shouldUseTwoPage(599, halfOpened = false))
        assertFalse(shouldUseTwoPage(360, halfOpened = false))
    }

    @Test
    fun halfOpenedForcesOnePage() {
        assertFalse(shouldUseTwoPage(800, halfOpened = true))
        assertFalse(shouldUseTwoPage(600, halfOpened = true))
    }
}
