package org.parkjw.apps.pdfbook.lock

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    const val ITERATIONS = 120_000
    const val SALT_BYTES = 16
    const val KEY_BITS = 256

    fun randomSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: CharArray, salt: ByteArray, iterations: Int, expected: ByteArray): Boolean {
        val actual = hash(pin, salt, iterations)
        return MessageDigest.isEqual(actual, expected)
    }
}

internal fun sameDigits(left: CharArray, leftLength: Int, right: CharArray, rightLength: Int): Boolean {
    if (leftLength != rightLength) return false
    var diff = 0
    for (index in 0 until leftLength) {
        diff = diff or (left[index].code xor right[index].code)
    }
    return diff == 0
}
