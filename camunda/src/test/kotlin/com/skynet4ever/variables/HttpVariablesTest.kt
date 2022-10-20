package com.skynet4ever.variables

import com.skynet4ever.spring.HttpProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.camunda.bpm.engine.delegate.VariableScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HttpVariablesTest {

    val objectMapper = ObjectMapper()

    lateinit var variableScope: VariableScope
    fun httpVariables(properties: HttpProperties = HttpProperties()) =
        HttpVariables(variableScope, objectMapper, properties)

    fun setMock(name: String, value: Any) {
        every { variableScope.hasVariable(name) } returns true
        every { variableScope.getVariable(name) } returns value
    }

    @BeforeEach
    fun setUp() {
        variableScope = mockk()
        every { variableScope.hasVariable(any()) } returns false
        every { variableScope.getVariable(any()) } returns null
        setMock("url", "someUrl")
    }

    @Test
    fun bodyFromMapVariable() {
        setMock("body", mapOf("some" to 123, "value" to mapOf("a" to "b")))
        assertEquals("""{"some":123,"value":{"a":"b"}}""", httpVariables().body)
    }

    @Test
    fun bodyFromStringVariable() {
        setMock("body", "someBody")
        assertEquals("someBody", httpVariables().body)
    }

    @Test
    fun defaultMethodGet() {
        assertEquals("GET", httpVariables().method)
    }

    @Test
    fun methodFromVariables() {
        setMock("method", "post")
        assertEquals("POST", httpVariables().method)
    }

    @Test
    fun headersDefault() {
        assertEquals(
            mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            ), httpVariables().headers
        )
    }

    @Test
    fun headersFromPropertiesMixedWithVariables() {
        val scopeHeaders = mapOf(
            "Accept" to "application/something",
            "instance" to "other-value",
            "shared" to "2",
            "Authorization" to "scope"
        )
        setMock("headers", scopeHeaders)

        val properties = HttpProperties(
            headers = mapOf(
                "Content-Type" to "application/other",
                "env" to "value",
                "shared" to "1",
                "authorization" to "env"
            )
        )

        val expected = mapOf(
            "Content-Type" to "application/other",
            "Accept" to "application/something",
            "env" to "value",
            "instance" to "other-value",
            "shared" to "2",
            "Authorization" to "scope"
        ).toSortedMap(String.CASE_INSENSITIVE_ORDER)

        val actualCaseInsensitive = httpVariables(properties).headers
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        assertEquals(expected, actualCaseInsensitive)
    }

    @Test
    fun authorizationOnInstanceOverridesOtherHeaders() {
        setMock("headers", mapOf("Authorization" to "scope"))
        setMock("authorization", "shortcut")

        val expected = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "Authorization" to "Bearer shortcut"
        ).toSortedMap(String.CASE_INSENSITIVE_ORDER)

        val actualCaseInsensitive = httpVariables().headers
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        assertEquals(expected, actualCaseInsensitive)
    }

    @Test
    fun successStatusCodeFromInstance() {
        setMock("successStatusCode", listOf(200, 201))
        assertEquals(setOf(200, 201), httpVariables().successStatusCode)
    }

    @Test
    fun successStatusCodeFromInstanceString() {
        setMock("successStatusCode", "[200, 201]")
        assertEquals(setOf(200, 201), httpVariables().successStatusCode)
    }

    @Test
    fun defaultSuccessStatusCode() {
        assertEquals(setOf(200), httpVariables().successStatusCode)
    }
}