package com.avtar.cabbilling.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the security PIN.
 *
 * The raw PIN is never stored — only [pinHash] (derived key) and [pinSalt].
 * Uses PBKDF2WithHmacSHA1, which is guaranteed present on every supported API
 * level (minSdk 24); the SHA-256 PRF variant only arrived on API 26.
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return encode(salt)
    }

    fun hash(pin: String, saltB64: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), decode(saltB64), ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            return encode(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison to avoid leaking match progress via timing. */
    fun verify(pin: String, saltB64: String, expectedHashB64: String): Boolean {
        if (saltB64.isEmpty() || expectedHashB64.isEmpty()) return false
        val actual = decode(hash(pin, saltB64))
        val expected = decode(expectedHashB64)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
