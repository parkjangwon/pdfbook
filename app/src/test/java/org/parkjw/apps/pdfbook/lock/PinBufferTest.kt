package org.parkjw.apps.pdfbook.lock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinBufferTest {
    @Test
    fun capsAtEightAndIgnoresNonDigits() {
        val b = PinBuffer()
        "12a3 -4".forEach { b.push(it) }
        assertEquals(4, b.length)
        "56789".forEach { b.push(it) }
        assertEquals(8, b.length)
        assertArrayEquals("12345678".toCharArray(), b.copyOut())
    }

    @Test
    fun popOnEmptyIsNoOpAndClearEmpties() {
        val b = PinBuffer()
        b.pop()
        assertEquals(0, b.length)
        "123".forEach { b.push(it) }
        b.pop()
        assertArrayEquals("12".toCharArray(), b.copyOut())
        b.clear()
        assertEquals(0, b.length)
        assertEquals(0, b.copyOut().size)
    }

    @Test
    fun copyOutIsIndependentOfBuffer() {
        val b = PinBuffer()
        "12".forEach { b.push(it) }
        b.copyOut().fill('0')
        assertArrayEquals("12".toCharArray(), b.copyOut())
    }
}

class PinCreateSessionTest {
    private fun PinCreateSession.type(s: String): CharArray? {
        var out: CharArray? = null
        s.forEach { out = digit(it) ?: out }
        return out
    }

    @Test
    fun confirmFirstRejectsTooShortAndSecondCall() {
        val s = PinCreateSession()
        s.type("123")
        assertFalse(s.confirmFirst())
        s.type("4")
        assertTrue(s.confirmFirst())
        assertTrue(s.confirming)
        assertFalse(s.confirmFirst())
    }

    @Test
    fun matchingConfirmReturnsPinAndResets() {
        val s = PinCreateSession()
        s.type("1357")
        s.confirmFirst()
        assertArrayEquals("1357".toCharArray(), s.type("1357"))
        assertFalse(s.confirming)
        assertEquals(0, s.length)
    }

    @Test
    fun mismatchReturnsToCreateWithFlagUntilNextDigit() {
        val s = PinCreateSession()
        s.type("1357")
        s.confirmFirst()
        assertNull(s.type("1358"))
        assertTrue(s.mismatch)
        assertFalse(s.confirming)
        assertEquals(0, s.firstLength)
        s.digit('1')
        assertFalse(s.mismatch)
    }

    @Test
    fun confirmIgnoresDigitsBeyondFirstLength() {
        val s = PinCreateSession()
        s.type("1234")
        s.confirmFirst()
        s.type("123")
        assertEquals(3, s.length)
        s.delete()
        assertEquals(2, s.length)
    }

    @Test
    fun backFromConfirmReturnsToCreateElseExits() {
        val s = PinCreateSession()
        s.type("1234")
        s.confirmFirst()
        assertFalse(s.back())
        assertFalse(s.confirming)
        assertEquals(0, s.length)
        assertTrue(s.back())
    }
}
