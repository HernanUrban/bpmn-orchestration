package com.skynet4ever

import net.jqwik.api.*
import org.apache.http.client.utils.URIBuilder
import org.junit.jupiter.api.Assertions.assertEquals

class HashTest {
    private val hash: Hash = Hash()

    @Property
    fun sha512LengthValidation(@ForAll value: String): Boolean =
        hash.sha512(value).length == 86

    @Property
    fun sha256LengthValidation(@ForAll value: String): Boolean =
        hash.sha256(value).length == 43

    @Property
    fun sameInputGeneratesTheSameOutput(
        @ForAll value: String,
        @ForAll("hashFunctions") hashFunction: (String) -> String
    ): Boolean =
        hashFunction(value) == hashFunction(value)

    @Property
    fun differentInputsGenerateDifferentOutputs(
        @ForAll valueA: String,
        @ForAll valueB: String,
        @ForAll("hashFunctions") hashFunction: (String) -> String
    ) {
        Assume.that(valueA != valueB)
        hashFunction(valueA) != hashFunction(valueB)
    }

    @Property
    fun `is URL safe for path and query string`(
        @ForAll value: String,
        @ForAll("hashFunctions") hashFunction: (String) -> String
    ) {
        val hashValue = hashFunction(value)
        val interpolatedUrl = "http://some.host/$hashValue/path?variable=$hashValue"
        val escapedUrl = URIBuilder("http://some.host")
            .setPath("/$hashValue/path")
            .addParameter("variable", hashValue)
            .toString()
        assertEquals(interpolatedUrl, escapedUrl)
    }

    @Provide
    fun hashFunctions(): Arbitrary<(String) -> String> =
        Arbitraries.of(hash::sha512, hash::sha256)

}