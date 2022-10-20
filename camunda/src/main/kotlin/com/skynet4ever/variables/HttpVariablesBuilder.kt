package com.skynet4ever.variables

import com.skynet4ever.spring.HttpProperties
import com.skynet4ever.utils.Logging
import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.delegate.VariableScope
import org.springframework.stereotype.Service

@Service
class HttpVariablesBuilder(
    private val objectMapper: ObjectMapper,
    private val httpProperties: HttpProperties
) : Logging {
    fun build(variableScope: VariableScope): HttpVariables {
        logger.trace("Http Properties: {}", httpProperties)
        return HttpVariables(variableScope, objectMapper, httpProperties)
    }
}