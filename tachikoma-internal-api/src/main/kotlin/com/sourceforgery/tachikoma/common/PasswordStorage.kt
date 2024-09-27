package com.sourceforgery.tachikoma.common

import com.google.common.io.BaseEncoding
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.experimental.xor

object PasswordStorage {
    val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"

    // These constants may be changed without breaking existing hashes.
    val SALT_BYTE_SIZE = 24
    val HASH_BYTE_SIZE = 18
    val PBKDF2_ITERATIONS = 64000

    // These constants define the encoding and may not be changed.
    val HASH_SECTIONS = 5
    val HASH_ALGORITHM_INDEX = 0
    val ITERATION_INDEX = 1
    val HASH_SIZE_INDEX = 2
    val SALT_INDEX = 3
    val PBKDF2_INDEX = 4

    class InvalidHashException : Exception {
        constructor(message: String) : super(message)
        constructor(message: String, source: Throwable) : super(message, source)
    }

    class CannotPerformOperationException : Exception {
        constructor(message: String) : super(message)
        constructor(message: String, source: Throwable) : super(message, source)
    }

    fun createHash(password: String): String {
        return createHash(password.toCharArray())
    }

    fun createHash(password: CharArray): String {
        // Generate a random salt
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTE_SIZE)
        random.nextBytes(salt)

        // Hash the password
        val hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
        val hashSize = hash.size

        // format: algorithm:iterations:hashSize:salt:hash
        return "sha1:" +
            PBKDF2_ITERATIONS +
            ":" + hashSize +
            ":" +
            toBase64(salt) +
            ":" +
            toBase64(hash)
    }

    fun verifyPassword(
        password: String,
        correctHash: String,
    ): Boolean {
        return verifyPassword(password.toCharArray(), correctHash)
    }

    fun verifyPassword(
        password: CharArray,
        correctHash: String,
    ): Boolean {
        // Decode the hash into its parameters
        val params = correctHash.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (params.size != HASH_SECTIONS) {
            throw InvalidHashException(
                "Fields are missing from the password hash.",
            )
        }

        // Currently, Java only supports SHA1.
        if (params[HASH_ALGORITHM_INDEX] != "sha1") {
            throw CannotPerformOperationException(
                "Unsupported hash type.",
            )
        }

        val iterations =
            try {
                Integer.parseInt(params[ITERATION_INDEX])
            } catch (ex: NumberFormatException) {
                throw InvalidHashException(
                    "Could not parse the iteration count as an integer.",
                    ex,
                )
            }

        if (iterations < 1) {
            throw InvalidHashException(
                "Invalid number of iterations. Must be >= 1.",
            )
        }

        val salt =
            try {
                fromBase64(params[SALT_INDEX])
            } catch (ex: IllegalArgumentException) {
                throw InvalidHashException(
                    "Base64 decoding of salt failed.",
                    ex,
                )
            }

        val hash =
            try {
                fromBase64(params[PBKDF2_INDEX])
            } catch (ex: IllegalArgumentException) {
                throw InvalidHashException(
                    "Base64 decoding of pbkdf2 output failed.",
                    ex,
                )
            }

        val storedHashSize =
            try {
                Integer.parseInt(params[HASH_SIZE_INDEX])
            } catch (ex: NumberFormatException) {
                throw InvalidHashException(
                    "Could not parse the hash size as an integer.",
                    ex,
                )
            }

        if (storedHashSize != hash.size) {
            throw InvalidHashException(
                "Hash length doesn't match stored hash length.",
            )
        }

        // Compute the hash of the provided password, using the same salt,
        // iteration count, and hash length
        val testHash = pbkdf2(password, salt, iterations, hash.size)
        // Compare the hashes in constant time. The password is correct if
        // both hashes match.
        return slowEquals(hash, testHash)
    }

    private fun slowEquals(
        a: ByteArray,
        b: ByteArray,
    ): Boolean {
        var diff = a.size xor b.size
        var i = 0
        while (i < a.size && i < b.size) {
            diff = diff or ((a[i]) xor (b[i])).toInt()
            i++
        }
        return diff == 0
    }

    private fun pbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        bytes: Int,
    ): ByteArray {
        try {
            val spec = PBEKeySpec(password, salt, iterations, bytes * 8)
            val skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            return skf.generateSecret(spec).encoded
        } catch (ex: NoSuchAlgorithmException) {
            throw CannotPerformOperationException(
                "Hash algorithm not supported.",
                ex,
            )
        } catch (ex: InvalidKeySpecException) {
            throw CannotPerformOperationException(
                "Invalid key spec.",
                ex,
            )
        }
    }

    private fun fromBase64(hex: String): ByteArray {
        return BaseEncoding.base16().decode(hex)
    }

    private fun toBase64(array: ByteArray): String {
        return BaseEncoding.base16().encode(array)
    }
}
