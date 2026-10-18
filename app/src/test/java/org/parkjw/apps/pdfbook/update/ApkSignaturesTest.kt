package org.parkjw.apps.pdfbook.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSignaturesTest {
    private val pkg = "org.parkjw.apps.pdfbook"
    private val release = sha256Hex("release-cert".toByteArray())
    private val debug = sha256Hex("debug-cert".toByteArray())
    private val rotated = sha256Hex("rotated-cert".toByteArray())

    private fun snap(vararg current: String, history: List<String> = current.toList(), name: String = pkg) =
        SignerSnapshot(name, current.toSet(), history)

    @Test
    fun sha256IsLowercaseHex() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun sameSignerMatches() {
        assertTrue(signaturesMatch(snap(release), snap(release)))
    }

    @Test
    fun differentSignerPackageOrMissingInfoIsRejected() {
        assertFalse(signaturesMatch(snap(release), snap(debug)))
        assertFalse(signaturesMatch(snap(release), snap(release, name = "com.example.other")))
        assertFalse(signaturesMatch(null, snap(release)))
        assertFalse(signaturesMatch(snap(release), null))
        assertFalse(signaturesMatch(snap(), snap()))
    }

    @Test
    fun rotatedKeyIsAcceptedOnlyWhenHistoryProvesIt() {
        assertTrue(signaturesMatch(snap(release), snap(rotated, history = listOf(release, rotated))))
        assertFalse(signaturesMatch(snap(release), snap(rotated, history = listOf(debug, rotated))))
    }

    @Test
    fun multipleSignersMustMatchExactly() {
        assertTrue(signaturesMatch(snap(release, debug, history = emptyList()), snap(debug, release, history = emptyList())))
        assertFalse(signaturesMatch(snap(release, debug, history = emptyList()), snap(release, history = emptyList())))
        assertFalse(signaturesMatch(snap(release, history = emptyList()), snap(release, rotated, history = emptyList())))
    }
}
