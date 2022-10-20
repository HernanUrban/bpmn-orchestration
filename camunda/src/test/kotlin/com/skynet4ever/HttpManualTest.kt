package com.skynet4ever

import com.skynet4ever.spring.Config
import com.skynet4ever.spring.HttpProperties
import com.skynet4ever.variables.HttpVariablesBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.camunda.bpm.engine.delegate.DelegateExecution

object HttpManualTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val execution = mockk<DelegateExecution>()
        every { execution.processBusinessKey } returns "test"
        every { execution.hasVariable(any()) } returns false
        every { execution.getVariable(any()) } returns null
        every { execution.getVariable("userId") } returns "894fa1eb-caac-4d7d-997c-b176b8f9abb9"
        every { execution.hasVariable("userId") } returns true
        every { execution.getVariable("url") } returns "https://webhook.site/{{userId}}"
        every { execution.hasVariable("url") } returns true
        every { execution.setVariableLocal(any(), any()) } returns Unit

        val config = Config()
        val http = HttpDelegate(
            RequestBuilder(),
            config.httpClient(HttpProperties()),
            HttpVariablesBuilder(config.objectMapper(), HttpProperties()),
            config.objectMapper(),
            SimpleMeterRegistry()
        )

        println("before")
        http.execute(execution)
        println("between")
        http.execute(execution)
        println("after")
    }
}