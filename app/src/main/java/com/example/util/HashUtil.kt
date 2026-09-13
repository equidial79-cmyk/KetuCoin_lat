package com.example.util

import java.security.MessageDigest

object HashUtil {
    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, hashedPin: String?): Boolean {
        if (hashedPin.isNullOrBlank()) return false
        return hashPin(pin).equals(hashedPin, ignoreCase = true)
    }
}
