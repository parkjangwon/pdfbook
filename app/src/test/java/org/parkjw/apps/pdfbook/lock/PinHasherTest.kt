package org.parkjw.apps.pdfbook.lock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    private val salt = ByteArray(PinHasher.SALT_BYTES) { it.toByte() }
    private val iterations = 1_000

    private fun hash(pin: String, salt: ByteArray = this.salt) =
        PinHasher.hash(pin.toCharArray(), salt, iterations)

    @Test
    fun sameInputsGiveSameHash() {
        assertArrayEquals(hash("1234"), hash("1234"))
    }

    @Test
    fun hashHasConfiguredKeyLength() {
        assertEquals(PinHasher.KEY_BITS / 8, hash("1234").size)
    }

    @Test
    fun differentPinOrSaltOrIterationsChangeHash() {
        val base = hash("1234")
        assertFalse(base.contentEquals(hash("1235")))
        assertFalse(base.contentEquals(hash("1234", ByteArray(PinHasher.SALT_BYTES) { 9 })))
        assertFalse(base.contentEquals(PinHasher.hash("1234".toCharArray(), salt, iterations + 1)))
    }

    @Test
    fun matchesAcceptsCorrectPinAndRejectsWrongOne() {
        val expected = hash("1234")
        assertTrue(PinHasher.matches("1234".toCharArray(), salt, iterations, expected))
        assertFalse(PinHasher.matches("4321".toCharArray(), salt, iterations, expected))
        assertFalse(PinHasher.matches("1234".toCharArray(), salt, iterations + 1, expected))
        assertFalse(PinHasher.matches("1234".toCharArray(), salt, iterations, expected.copyOf(8)))
    }

    @Test
    fun hashDoesNotContainPlaintextPin() {
        val text = String(hash("1234"), Charsets.ISO_8859_1)
        assertFalse(text.contains("1234"))
    }

    @Test
    fun hashDoesNotMutateInputPin() {
        val pin = "1234".toCharArray()
        PinHasher.hash(pin, salt, iterations)
        assertArrayEquals("1234".toCharArray(), pin)
    }

    @Test
    fun randomSaltHasExpectedSizeAndVaries() {
        val a = PinHasher.randomSalt()
        val b = PinHasher.randomSalt()
        assertEquals(PinHasher.SALT_BYTES, a.size)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun sameDigitsComparesLengthAndContent() {
        assertTrue(sameDigits("1234".toCharArray(), 4, "1234".toCharArray(), 4))
        assertFalse(sameDigits("1234".toCharArray(), 4, "1235".toCharArray(), 4))
        assertFalse(sameDigits("1234".toCharArray(), 4, "12345".toCharArray(), 5))
        assertTrue(sameDigits("12349".toCharArray(), 4, "1234".toCharArray(), 4))
    }
}
