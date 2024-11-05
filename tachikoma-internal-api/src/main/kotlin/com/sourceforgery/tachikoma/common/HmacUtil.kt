package com.sourceforgery.tachikoma.common

import java.math.BigInteger
import java.security.MessageDigest

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

fun calculateMd5(bytes: ByteArray): String {
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(bytes)
    return String.format("%032x", BigInteger(1, md5.digest()))
}
