package org.parkjw.apps.pdfbook.lock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LockRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun open(): Pair<File, LockRepository> {
        val file = File(tmp.newFolder(), "lock.preferences_pb")
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        return file to LockRepository(store)
    }

    @Test
    fun l1_emptyLockStoreIsNone() = runBlocking {
        val (_, repo) = open()
        assertEquals(LockConfig(), repo.config.first())
        assertEquals(AppLockMode.None, repo.config.first().mode)
    }

    @Test
    fun l5_repositoryRejectsBiometricWithoutPin() = runBlocking {
        val (_, repo) = open()
        assertFalse(repo.setMode(AppLockMode.Biometric))
        assertEquals(AppLockMode.None, repo.config.first().mode)
        assertFalse(repo.setMode(AppLockMode.Pin))
    }

    @Test
    fun l6_pinIsHashedNotStoredAndVerifyMatches() = runBlocking {
        val secret = "91827364"
        val (file, repo) = open()
        val pin = secret.toCharArray()
        repo.saveNewPin(pin, AppLockMode.Pin)
        assertTrue(pin.all { it == '\u0000' })
        val saved = repo.config.first()
        assertEquals(AppLockMode.Pin, saved.mode)
        assertEquals(PinHasher.ITERATIONS, saved.pinIterations)
        assertTrue(saved.pinIterations >= 100_000)
        assertEquals(8, saved.pinLength)
        assertEquals(32, java.util.Base64.getDecoder().decode(saved.pinHash).size)
        assertEquals(16, java.util.Base64.getDecoder().decode(saved.pinSalt).size)
        assertTrue(saved.hideInRecents)
        val prefsText = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(prefsText.contains(secret))
        assertFalse(saved.pinHash!!.contains(secret))
        assertFalse(saved.pinSalt!!.contains(secret))
        val again = open().second
        again.saveNewPin(secret.toCharArray(), AppLockMode.Pin)
        assertNotEquals(saved.pinHash, again.config.first().pinHash)
        val saltA = PinHasher.randomSalt()
        val saltB = PinHasher.randomSalt()
        val sample = secret.toCharArray()
        assertFalse(PinHasher.hash(sample, saltA).contentEquals(PinHasher.hash(sample, saltB)))
        assertTrue(PinHasher.matches(secret.toCharArray(), saltA, PinHasher.ITERATIONS, PinHasher.hash(secret.toCharArray(), saltA)))
        assertFalse(PinHasher.matches("00000000".toCharArray(), saltA, PinHasher.ITERATIONS, PinHasher.hash(secret.toCharArray(), saltA)))
        val (storeFile, check) = open()
        check.saveNewPin(secret.toCharArray(), AppLockMode.Pin)
        assertEquals(PinVerify.Success, check.verifyPin(secret.toCharArray(), 1_000L))
        assertEquals(PinVerify.Wrong, check.verifyPin("00000000".toCharArray(), 2_000L))
        assertFalse(storeFile.readBytes().toString(Charsets.ISO_8859_1).contains(secret))
        assertTrue(check.setMode(AppLockMode.None))
        val cleared = check.config.first()
        assertEquals(AppLockMode.None, cleared.mode)
        assertFalse(cleared.hasPin)
        assertEquals(0, cleared.failedAttempts)
    }

    @Test
    fun l7_fiveFailuresLockOutAndIgnoreCorrectPinAndClockRollback() = runBlocking {
        val (_, repo) = open()
        repo.saveNewPin("1357".toCharArray(), AppLockMode.Pin)
        val start = 10_000L
        repeat(4) { index ->
            assertEquals(PinVerify.Wrong, repo.verifyPin("0000".toCharArray(), start + index))
        }
        assertEquals(0L, repo.config.first().lockoutUntilEpochMs)
        assertEquals(PinVerify.Wrong, repo.verifyPin("0000".toCharArray(), start + 4))
        val locked = repo.config.first()
        assertEquals(5, locked.failedAttempts)
        assertEquals(start + 4 + 30_000L, locked.lockoutUntilEpochMs)
        val skewedNow = locked.lockoutUntilEpochMs - 500_000L
        assertEquals(30_000L, remainingLockoutMs(skewedNow, locked.lockoutUntilEpochMs, locked.failedAttempts))
        val waiting = repo.verifyPin("1357".toCharArray(), skewedNow)
        assertTrue(waiting is PinVerify.Waiting)
        assertEquals(30_000L, (waiting as PinVerify.Waiting).remainingMs)
        assertEquals(5, repo.config.first().failedAttempts)
        assertEquals(PinVerify.Success, repo.verifyPin("1357".toCharArray(), locked.lockoutUntilEpochMs))
        assertEquals(0, repo.config.first().failedAttempts)
    }
}
