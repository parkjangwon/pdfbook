package org.parkjw.apps.pdfbook.lock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCreateSessionTest {
    private fun PinCreateSession.type(digits: String): CharArray? {
        var result: CharArray? = null
        digits.forEach { result = digit(it) }
        return result
    }

    @Test
    fun matchingConfirmationReturnsPinAndResets() {
        val session = PinCreateSession()
        session.type("1234")
        assertTrue(session.confirmFirst())
        assertTrue(session.confirming)
        assertEquals(4, session.firstLength)
        assertEquals(0, session.length)
        assertNull(session.type("123"))
        assertArrayEquals("1234".toCharArray(), session.digit('4'))
        assertFalse(session.confirming)
        assertEquals(0, session.length)
        assertEquals(0, session.firstLength)
        assertFalse(session.mismatch)
    }

    @Test
    fun mismatchResetsToFirstStepAndFlagsMismatch() {
        val session = PinCreateSession()
        session.type("1234")
        session.confirmFirst()
        assertNull(session.type("1235"))
        assertTrue(session.mismatch)
        assertFalse(session.confirming)
        assertEquals(0, session.length)
        assertEquals(0, session.firstLength)
        session.digit('9')
        assertFalse(session.mismatch)
        assertEquals(1, session.length)
    }

    @Test
    fun firstEntryIsCappedAtEightDigits() {
        val session = PinCreateSession()
        session.type("1234567899")
        assertEquals(8, session.length)
    }

    @Test
    fun confirmFirstRequiresFourToEightDigits() {
        val session = PinCreateSession()
        session.type("123")
        assertFalse(session.confirmFirst())
        assertFalse(session.confirming)
        session.digit('4')
        assertTrue(session.confirmFirst())
    }

    @Test
    fun confirmFirstIsIgnoredWhileConfirming() {
        val session = PinCreateSession()
        session.type("123456")
        session.confirmFirst()
        session.type("1234")
        assertFalse(session.confirmFirst())
        assertEquals(4, session.length)
        assertEquals(6, session.firstLength)
    }

    @Test
    fun eightDigitPinRoundTrips() {
        val session = PinCreateSession()
        session.type("12345678")
        session.confirmFirst()
        assertArrayEquals("12345678".toCharArray(), session.type("12345678"))
    }

    @Test
    fun confirmationInputStopsAtFirstLength() {
        val session = PinCreateSession()
        session.type("1234")
        session.confirmFirst()
        session.type("12")
        assertEquals(2, session.length)
    }

    @Test
    fun deleteRemovesLastDigitAndIsSafeWhenEmpty() {
        val session = PinCreateSession()
        session.delete()
        assertEquals(0, session.length)
        session.type("12")
        session.delete()
        assertEquals(1, session.length)
        session.type("234")
        assertTrue(session.confirmFirst())
        assertArrayEquals("1234".toCharArray(), session.type("1234"))
    }

    @Test
    fun deleteWorksDuringConfirmation() {
        val session = PinCreateSession()
        session.type("1234")
        session.confirmFirst()
        session.type("12")
        session.delete()
        session.delete()
        assertEquals(0, session.length)
        assertArrayEquals("1234".toCharArray(), session.type("1234"))
    }

    @Test
    fun backFromFirstStepClearsAndReportsExit() {
        val session = PinCreateSession()
        session.type("12")
        assertTrue(session.back())
        assertEquals(0, session.length)
    }

    @Test
    fun backFromConfirmReturnsToFirstStepWithoutExit() {
        val session = PinCreateSession()
        session.type("1234")
        session.confirmFirst()
        session.type("12")
        assertFalse(session.back())
        assertFalse(session.confirming)
        assertEquals(0, session.length)
        assertEquals(0, session.firstLength)
        assertFalse(session.mismatch)
    }

    @Test
    fun backAfterMismatchClearsFlag() {
        val session = PinCreateSession()
        session.type("1234")
        session.confirmFirst()
        session.type("0000")
        assertTrue(session.mismatch)
        assertTrue(session.back())
        assertFalse(session.mismatch)
    }
}
