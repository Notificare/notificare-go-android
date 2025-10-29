package com.actito.go.core

import java.security.MessageDigest

internal fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(input.toByteArray()).toHex()
}

private fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte -> String.format("%02x", byte) }
}
