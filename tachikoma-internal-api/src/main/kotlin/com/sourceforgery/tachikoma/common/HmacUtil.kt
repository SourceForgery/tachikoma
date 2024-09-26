package com.sourceforgery.tachikoma.common

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtil {
    private fun slowEquals(
        a: CharSequence,
        b: CharSequence,
    ): Boolean {
        var diff = a.length xor b.length
        var i = 0
        while (i < a.length && i < b.length) {
            diff = diff or (a[i].code xor b[i].code)
            i++
        }
        return diff == 0
    }

    fun hmacSha1(
        data: ByteArray,
        key: ByteArray,
    ): ByteArray {
        return hmac(data, key, "HmacSHA1")
    }

    fun hmac(
        data: ByteArray,
        key: ByteArray,
        algorithm: String,
    ): ByteArray {
        val signingKey = SecretKeySpec(key, algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(signingKey)

        return mac.doFinal(data)
    }

    fun calculateHmacSha256URLSafeNoPadding(
        data: ByteArray,
        key: ByteArray,
    ) = hmac(data, key, "HmacSHA256")

    fun calculateMd5(sText: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(sText.toByteArray(StandardCharsets.UTF_8))

        return String.format("%032x", BigInteger(1, md5.digest()))
    }

    fun calculateMd5(bytes: ByteArray): String {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(bytes)
        return String.format("%032x", BigInteger(1, md5.digest()))
    }
}
