package com.skynet4ever.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MeteredJavaDelegateTest {

    class SomeDelegate(val f: () -> Unit, registry: MeterRegistry) : MeteredJavaDelegate(registry) {
        override fun meteredExecute(execution: DelegateExecution) {
            f()
        }
    }

    val context: DelegateExecution = mockk()
    val registry = PrometheusMeterRegistry { null }
    val execute: () -> Unit = mockk()
    val delegate = SomeDelegate(execute, registry)

    @BeforeEach
    fun setUp() {
        every { context.processDefinitionId } returns "defId"
        every { context.currentActivityId } returns "activityId"
        every {
            context.processEngineServices
                .repositoryService
                .getProcessDefinition("defId")
                .key
        } returns "defKey"
    }

    @Test
    fun canMeasureSuccess() {
        justRun { execute.invoke() }
        delegate.execute(context)
        assertEquals(5, prometheusCount())
    }

    @Test
    fun canMeasureError() {
        every { execute.invoke() } throws BpmnError("code")
        assertThrows<BpmnError> {
            delegate.execute(context)
        }
        assertEquals(5, prometheusCount())
    }

    class SomeException : Exception("something")

    @Test
    fun canMeasureFailure() {
        every { execute.invoke() } throws SomeException()
        assertThrows<SomeException> {
            delegate.execute(context)
        }
        assertEquals(5, prometheusCount())
    }

    @Test
    fun canMeasureAllTogether() {
        justRun { execute.invoke() }
        delegate.execute(context)

        every { execute.invoke() } throws BpmnError("code")
        assertThrows<BpmnError> {
            delegate.execute(context)
        }

        every { execute.invoke() } throws SomeException()
        assertThrows<SomeException> {
            delegate.execute(context)
        }

        assertEquals(11, prometheusCount())
    }

    /**
     * Metrics over the plain registry doesn't reflect the limitations of PrometheusRegistry
     * (e.g.: can't have multiple metrics with the same name but different number of tags)
     */
    private fun prometheusCount() =
        registry.prometheusRegistry
            .metricFamilySamples()
            .toList()
            .flatMap { it.samples }
            .size
}