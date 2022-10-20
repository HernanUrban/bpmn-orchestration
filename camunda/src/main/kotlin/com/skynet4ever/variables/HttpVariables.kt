package com.skynet4ever.variables

import com.skynet4ever.spring.HttpProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.delegate.VariableScope

class HttpVariables(
    private val variableScope: VariableScope,
    private val objectMapper: ObjectMapper,
    private val httpProperties: HttpProperties
) {

    companion object {
        private val successStatusCodeSetType = object : TypeReference<Set<Int>>() {}
    }

    private fun scopeVariable(name: String): Any? =
        if (variableScope.hasVariable(name) && variableScope.getVariable(name) != null)
            variableScope.getVariable(name)
        else
            null

    private fun string(name: String): String? =
        scopeVariable(name) as String?

    val url: String = string("url") ?: throw RequiredVariableException("url")

    val body: String? = scopeVariable("body")?.let {
        // if it is a string consider it as a valid payload as is
        if (it is String) it
        // serialize any other type as json
        else objectMapper.writeValueAsString(variableScope.getVariable("body"))
    }

    val method: String = string("method")?.toUpperCase() ?: "GET"

    // only used by the "headers" variable
    private val authorization: String? = string("authorization")

    val headers: Map<String, String> by lazy {
        val defaults = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        ).toSortedMap(String.CASE_INSENSITIVE_ORDER)

        // put env variables
        httpProperties.headers.let { defaults.putAll(it) }

        // put flow variables
        scopeVariable("headers")?.let {
            @Suppress("UNCHECKED_CAST")
            defaults.putAll(it as Map<String, String>)
        }

        // override authorization header if "authorization" was set individually
        if (authorization != null) {
            defaults["Authorization"] = "Bearer $authorization"
        }
        defaults
    }

    val readTimeoutSecs: Long =
        scopeVariable("readTimeoutSecs") as Long? ?: httpProperties.readTimeoutSecs

    val successStatusCode: Set<Int> by lazy {
        scopeVariable("successStatusCode")?.let {
            // try to parse it from a collection
            if (it is Collection<Any?>)
                when {
                    it.isEmpty() -> emptySet()
                    it.first() is Number -> it.map { a -> (a as Number).toInt() }.toSet()
                    else -> throw ClassCastException("Can't convert 'successStatusCode' to Set<Long>: '$it'.")
                }
            else
            // try to parse it from a string
                objectMapper.readValue(it as String, successStatusCodeSetType)
        } ?: setOf(200)
    }

    @Suppress("UNCHECKED_CAST")
    val expectedErrorCodes: Set<String> =
        (scopeVariable("expectedErrorCodes") as List<String>?)?.toSet() ?: emptySet()

}

class RequiredVariableException(name: String) :
    RuntimeException("The variable '$name' was not defined and it's required.")
