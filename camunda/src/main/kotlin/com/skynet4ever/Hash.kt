package com.skynet4ever

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*

@Service("Hash")
class Hash {
    fun sha512(input: String) = hashString("SHA-512", input)

    fun sha256(input: String) = hashString("SHA-256", input)

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /**
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    private fun hashString(type: String, input: String): String {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())

        return encoder.encodeToString(bytes)
    }
}