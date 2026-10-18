package org.parkjw.apps.pdfbook.lock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.lockDataStore by preferencesDataStore(name = "lock")

sealed interface PinVerify {
    data object Success : PinVerify

    data object Wrong : PinVerify

    data class Waiting(val remainingMs: Long) : PinVerify
}

class LockRepository(
    private val dataStore: DataStore<Preferences>,
    private val hashDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    constructor(context: Context) : this(context.lockDataStore)

    val config: Flow<LockConfig> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toLockConfig() }

    suspend fun saveNewPin(pin: CharArray, mode: AppLockMode) {
        try {
            require(isValidPin(pin))
            require(mode == AppLockMode.Pin || mode == AppLockMode.Biometric)
            val salt = PinHasher.randomSalt()
            val iterations = PinHasher.ITERATIONS
            val hash = withContext(hashDispatcher) { PinHasher.hash(pin, salt, iterations) }
            val length = pin.size
            dataStore.edit { prefs ->
                val enabling = prefs.toLockConfig().mode == AppLockMode.None
                prefs[ModeKey] = mode.name
                prefs[PinHashKey] = Base64.getEncoder().encodeToString(hash)
                prefs[PinSaltKey] = Base64.getEncoder().encodeToString(salt)
                prefs[PinIterationsKey] = iterations
                prefs[PinLengthKey] = length
                prefs[FailedAttemptsKey] = 0
                prefs[LockoutUntilKey] = 0L
                if (enabling) prefs[HideInRecentsKey] = true
            }
        } finally {
            pin.fill('\u0000')
        }
    }

    suspend fun setMode(mode: AppLockMode): Boolean {
        val before = config.first()
        if (mode != AppLockMode.None && !before.hasPin) return false
        if (mode == AppLockMode.Biometric && !canSelectBiometric(before.hasPin)) return false
        dataStore.edit { prefs ->
            val current = prefs.toLockConfig()
            if (mode == AppLockMode.None) {
                prefs.remove(PinHashKey)
                prefs.remove(PinSaltKey)
                prefs.remove(PinIterationsKey)
                prefs.remove(PinLengthKey)
                prefs[FailedAttemptsKey] = 0
                prefs[LockoutUntilKey] = 0L
                prefs[ModeKey] = AppLockMode.None.name
                return@edit
            }
            if (current.mode == AppLockMode.None) prefs[HideInRecentsKey] = true
            prefs[ModeKey] = mode.name
        }
        return config.first().mode == mode
    }

    suspend fun setHideInRecents(hide: Boolean) {
        dataStore.edit { it[HideInRecentsKey] = hide }
    }

    suspend fun verifyPin(pin: CharArray, nowEpochMs: Long): PinVerify {
        try {
            val current = config.first()
            val remaining = remainingLockoutMs(nowEpochMs, current.lockoutUntilEpochMs, current.failedAttempts)
            if (remaining > 0L) return PinVerify.Waiting(remaining)
            if (!current.hasPin) return PinVerify.Wrong
            val salt = Base64.getDecoder().decode(current.pinSalt)
            val expected = Base64.getDecoder().decode(current.pinHash)
            val ok = withContext(hashDispatcher) {
                PinHasher.matches(pin, salt, current.pinIterations, expected)
            }
            if (ok) {
                dataStore.edit {
                    it[FailedAttemptsKey] = 0
                    it[LockoutUntilKey] = 0L
                }
                return PinVerify.Success
            }
            val attempts = current.failedAttempts + 1
            val wait = lockoutMillisFor(attempts)
            dataStore.edit {
                it[FailedAttemptsKey] = attempts
                it[LockoutUntilKey] = if (wait == 0L) 0L else nowEpochMs + wait
            }
            return PinVerify.Wrong
        } finally {
            pin.fill('\u0000')
        }
    }

    companion object {
        val ModeKey = stringPreferencesKey("mode")
        val PinHashKey = stringPreferencesKey("pin_hash")
        val PinSaltKey = stringPreferencesKey("pin_salt")
        val PinIterationsKey = intPreferencesKey("pin_iterations")
        val PinLengthKey = intPreferencesKey("pin_length")
        val HideInRecentsKey = booleanPreferencesKey("hide_in_recents")
        val FailedAttemptsKey = intPreferencesKey("failed_attempts")
        val LockoutUntilKey = longPreferencesKey("lockout_until_epoch_ms")
    }
}

internal fun Preferences.toLockConfig(): LockConfig {
    val defaults = LockConfig()
    val mode = when (this[LockRepository.ModeKey]) {
        AppLockMode.Pin.name -> AppLockMode.Pin
        AppLockMode.Biometric.name -> AppLockMode.Biometric
        else -> AppLockMode.None
    }
    return LockConfig(
        mode = mode,
        pinHash = this[LockRepository.PinHashKey],
        pinSalt = this[LockRepository.PinSaltKey],
        pinIterations = this[LockRepository.PinIterationsKey] ?: defaults.pinIterations,
        pinLength = this[LockRepository.PinLengthKey] ?: defaults.pinLength,
        hideInRecents = this[LockRepository.HideInRecentsKey] ?: defaults.hideInRecents,
        failedAttempts = this[LockRepository.FailedAttemptsKey] ?: defaults.failedAttempts,
        lockoutUntilEpochMs = this[LockRepository.LockoutUntilKey] ?: defaults.lockoutUntilEpochMs,
    )
}
