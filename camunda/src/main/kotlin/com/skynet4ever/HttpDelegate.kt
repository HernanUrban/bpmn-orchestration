package com.skynet4ever

import com.skynet4ever.utils.Logging
import com.skynet4ever.utils.MeteredJavaDelegate
import com.skynet4ever.variables.HttpVariables
import com.skynet4ever.variables.HttpVariablesBuilder
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Service
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.charset.StandardCharsets

@Service("http")
open class HttpDelegate(
    private val requestBuilder: RequestBuilder,
    private val client: HttpClient,
    private val httpVariablesBuilder: HttpVariablesBuilder,
    private val objectMapper: ObjectMapper,
    private val registry: MeterRegistry
) : MeteredJavaDelegate(registry) {

    companion object : Logging {
        private val stringBody: BodyHandler<String> = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        private val mapType = object : TypeReference<Map<String, Any>>() {}
        private val errorResponseType = object : TypeReference<ErrorResponse>() {}
    }

    override fun meteredExecute(execution: DelegateExecution) {
        return try {
            // TODO log full request response (including body) on "trace" level
            logger.info(
                "Http service task started, " +
                        "business key: '{}', " +
                        "activityInstanceId: '{}'",
                execution.processBusinessKey,
                execution.activityInstanceId
            )
            val variables = httpVariablesBuilder.build(execution)
            val request = requestBuilder.build(variables)

            logger.debug("HTTP request {} {}", request.method(), request.uri())
            val response = client.send(request, stringBody)
            logger.debug("HTTP response, status code {}", response.statusCode())

            val statusCode = response.statusCode()
            execution.setVariableLocal("statusCode", statusCode)

            val rawBody = response.body()
            
            // body size metric
            registry.summary("camunda.delegate.body.size", *metricTags(execution))
                .record(rawBody?.length?.toDouble() ?: 0.0)
            
            // set the response as a process instance variable
            val body = parseBody(rawBody, emptyMap(), mapType)
            execution.setVariableLocal("body", body)

            if (isSuccessful(variables, statusCode)) {
                logger.info(
                    "Service task successful execution, " +
                            "business key: '{}', " +
                            "activityInstanceId: '{}'",
                    execution.processBusinessKey,
                    execution.activityInstanceId
                )
                // do nothing (the body and status code was already set as variable)
            } else {
                logger.warn(
                    "Response status code wasn't successful (" +
                            "status code: '{}', " +
                            "successful status codes: '{}', " +
                            "business key: '{}', " +
                            "activityInstanceId: '{}')",
                    statusCode,
                    variables.successStatusCode,
                    execution.processBusinessKey,
                    execution.activityInstanceId
                )
                throw buildDelegateException(response.body(), variables)
            }
        } catch (e: Throwable) {
            logger.warn(
                "Service task failed execution, " +
                        "exception message: '{}', " +
                        "business key: '{}', " +
                        "activityInstanceId: '{}'",
                e.message,
                execution.processBusinessKey,
                execution.activityInstanceId
            )
            throw e
        }
    }

    private fun buildDelegateException(rawBody: String?, variables: HttpVariables): Exception {
        val errorResponse = parseBody(rawBody, ErrorResponse(null), errorResponseType)
        return if (
            errorResponse.error?.code != null &&
            variables.expectedErrorCodes.contains(errorResponse.error.code)
        ) {
            BpmnError(
                errorResponse.error.code,
                errorResponse.error.message
            )
        } else {
            UnsuccessfulResponseException(rawBody)
        }
    }

    private fun isSuccessful(variables: HttpVariables, statusCode: Int) =
        variables.successStatusCode.contains(statusCode)

    private fun <T> parseBody(rawBody: String?, empty: T, typeReference: TypeReference<T>): T =
        if (rawBody.isNullOrBlank()) {
            logger.trace("Response body is null or blank '{}'", rawBody)
            empty
        } else {
            try {
                objectMapper.readValue(rawBody, typeReference)
            } catch (e: JsonParseException) {
                throw InvalidJsonResponse(rawBody, e)
            }
        }

}

data class ErrorResponse(val error: Error?)
data class Error(val code: String?, val message: String?)

data class UnsuccessfulResponseException(val rawBody: String?) :
    RuntimeException("Response body: '$rawBody'")

data class InvalidJsonResponse(val rawBody: String, override val cause: Throwable) :
    RuntimeException("Invalid JSON response: '$rawBody'", cause)