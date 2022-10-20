package com.skynet4ever

import com.skynet4ever.variables.HttpVariables
import com.skynet4ever.variables.HttpVariablesBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpDelegateTest {

    val request: HttpRequest = mockk()
    val response: HttpResponse<String> = mockk()
    val requestBuilder: RequestBuilder = mockk()
    val httpClient: HttpClient = mockk()
    val variables: HttpVariables = mockk()
    val httpVariablesBuilder: HttpVariablesBuilder = mockk()
    val delegate: HttpDelegate = HttpDelegate(
        requestBuilder,
        httpClient,
        httpVariablesBuilder,
        ObjectMapper().registerModule(KotlinModule()),
        SimpleMeterRegistry()
    )
    val execution: DelegateExecution = mockk(relaxed = true)
    val statusCodeVariable = "statusCode"
    val bodyVariable = "body"

    @BeforeEach
    fun setUp() {
        every { requestBuilder.build(any()) } returns request
        every { request.method() } returns "GET"
        every { request.uri() } returns URI("https://something/value")
        every { httpVariablesBuilder.build(any()) } returns variables
        every { execution.processBusinessKey } returns "123"
    }

    @Test
    fun setStatusCodeOnSuccessAndEmptyBody() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200)
        every { response.statusCode() } returns 200
        every { response.body() } returns ""

        delegate.execute(execution)

        verify { execution.setVariableLocal(statusCodeVariable, 200) }
        verify { execution.setVariableLocal(bodyVariable, emptyMap<String, Any>()) }
    }

    @Test
    fun setBodyOnSuccess() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200)
        every { response.statusCode() } returns 200
        every { response.body() } returns """{"some":"thing"}"""

        delegate.execute(execution)

        verify { execution.setVariableLocal(statusCodeVariable, 200) }
        verify { execution.setVariableLocal(bodyVariable, mapOf("some" to "thing")) }
    }

    @Test
    fun setBodyOnHandledFailures() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200, 500)
        every { response.statusCode() } returns 500
        every { response.body() } returns """{"some":"thing"}"""

        delegate.execute(execution)

        verify { execution.setVariableLocal(statusCodeVariable, 500) }
        verify { execution.setVariableLocal(bodyVariable, mapOf("some" to "thing")) }
    }

    @Test
    fun throwBpmnErrorOnFailure() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200)
        every { variables.expectedErrorCodes } returns setOf("other.code", "some.code")
        every { response.statusCode() } returns 500
        every { response.body() } returns """{"error":{"code":"some.code","message":"some message"}}"""

        try {
            delegate.execute(execution)
            fail("should throw error")
        } catch (e: BpmnError) {
            Assertions.assertEquals("some.code", e.errorCode)
            Assertions.assertEquals("some message", e.message)
        }
    }

    @Test
    fun throwUnsuccessfulResponseExceptionOnFailure() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200)
        every { variables.expectedErrorCodes } returns emptySet()
        every { response.statusCode() } returns 500
        val body = """{"error":{"code":"some.code","message":"some message"}}"""
        every { response.body() } returns body

        try {
            delegate.execute(execution)
            fail("should throw error")
        } catch (e: UnsuccessfulResponseException) {
            Assertions.assertEquals("Response body: '$body'", e.message)
        }
    }

    @Test
    fun throwInvalidJsonResponseOnFailure() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } returns response
        every { variables.successStatusCode } returns setOf(200)
        every { variables.expectedErrorCodes } returns emptySet()
        every { response.statusCode() } returns 500
        val body = "some non-json response"
        every { response.body() } returns body

        try {
            delegate.execute(execution)
            fail("should throw error")
        } catch (e: InvalidJsonResponse) {
            Assertions.assertEquals("Invalid JSON response: '$body'", e.message)
        }
    }

    @Test
    fun reThrowOtherExceptions() {
        every { httpClient.send(any(), any<HttpResponse.BodyHandler<String>>()) } throws CustomException
        every { variables.successStatusCode } returns setOf(200)
        every { response.statusCode() } returns 500
        every { response.body() } returns ""

        assertThrows<CustomException> {
            delegate.execute(execution)
        }
    }

    object CustomException : RuntimeException("something")
}