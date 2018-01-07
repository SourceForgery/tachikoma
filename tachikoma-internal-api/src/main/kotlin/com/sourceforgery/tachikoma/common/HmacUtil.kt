package com.sourceforgery.tachikoma.common

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

object HmacUtil {
    private fun slowEquals(a: CharSequence, b: CharSequence): Boolean {
        var diff = a.length xor b.length
        var i = 0
        while (i < a.length && i < b.length) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
            i++
        }
        return diff == 0
    }

    fun hmacSha1(data: ByteArray, key: ByteArray): String {
        return hmac(data, key, "HmacSHA1")
    }

    fun hmac(data: ByteArray, key: ByteArray, algorithm: String, urlSafe: Boolean = false): String {
        val signingKey = SecretKeySpec(key, algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(signingKey)

        val rawHmac = mac.doFinal(data)

        return if (urlSafe) {
            Base64.getUrlEncoder()
        } else {
            Base64.getEncoder()
        }.encodeToString(rawHmac)
    }

    fun validateHmacSha1(data: ByteArray, key: ByteArray, expectedString: String): Boolean {
        val calculatedHmacSha1 = hmacSha1(data, key)
        return slowEquals(calculatedHmacSha1, expectedString)
    }

    fun calculateHmacSha256URLSafeNoPadding(data: ByteArray, key: ByteArray) =
            hmac(data, key, "HmacSHA256", true)

    fun calculateMd5(sText: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(sText.toByteArray(StandardCharsets.UTF_8))

        return String.format("%032x", BigInteger(1, md5.digest()))
    }
}